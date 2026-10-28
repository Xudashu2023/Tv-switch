package com.tvswitch.app.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/**
 * 最小 ADB 客户端（原创实现，公开协议）。
 * 帧格式：24 字节头（小端）= 命令、arg0、arg1、数据长度、数据校验和、magic。
 * 校验和 = 数据字节之和；magic = 命令 xor 0xffffffff。
 * 命令：CNXN 0x4e584e43，AUTH 0x48545541，OPEN 0x4e45504f，
 * OKAY 0x59414b4f，CLSE 0x45534c43，WRTE 0x45545257。
 */
class AdbClient(private val keys: AdbKeyStore.Keys, private val keyStore: AdbKeyStore) {

    companion object {
        const val CMD_CNXN = 0x4e584e43
        const val CMD_AUTH = 0x48545541
        const val CMD_OPEN = 0x4e45504f
        const val CMD_OKAY = 0x59414b4f
        const val CMD_CLSE = 0x45534c43
        const val CMD_WRTE = 0x45545257
        const val VERSION = 0x01000000
        const val MAX_DATA = 256 * 1024
        const val AUTH_TOKEN = 1
        const val AUTH_SIGNATURE = 2
        const val AUTH_RSA_PUBLIC = 3
    }

    private data class Msg(val cmd: Int, val arg0: Int, val arg1: Int, val data: ByteArray)

    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private val nextLocalId = AtomicInteger(1)
    private val lock = Any()

    @Volatile var connected: Boolean = false
        private set

    var lastError: String = ""
        private set

    private fun send(cmd: Int, arg0: Int, arg1: Int, data: ByteArray = ByteArray(0)) {
        synchronized(lock) {
            val out = output ?: throw IllegalStateException("未连接")
            val checksum = data.fold(0) { acc, b -> acc + (b.toInt() and 0xff) }
            val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            header.putInt(cmd)
            header.putInt(arg0)
            header.putInt(arg1)
            header.putInt(data.size)
            header.putInt(checksum)
            header.putInt(cmd xor -1)
            out.write(header.array())
            if (data.isNotEmpty()) out.write(data)
            out.flush()
        }
    }

    private fun readMsg(): Msg {
        val inp = input ?: throw IllegalStateException("未连接")
        val header = ByteArray(24)
        inp.readFully(header)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = bb.int
        val arg0 = bb.int
        val arg1 = bb.int
        val len = bb.int
        bb.int // checksum，读取后不强制校验（部分实现会省略）
        bb.int // magic
        val data = if (len > 0) {
            val d = ByteArray(len)
            inp.readFully(d)
            d
        } else ByteArray(0)
        return Msg(cmd, arg0, arg1, data)
    }

    suspend fun connect(host: String, port: Int, timeoutMs: Int = 10000): Boolean =
        withContext(Dispatchers.IO) {
            try {
                disconnectQuietly()
                val sock = Socket()
                sock.connect(InetSocketAddress(host, port), timeoutMs)
                sock.soTimeout = timeoutMs
                socket = sock
                input = DataInputStream(sock.getInputStream())
                output = DataOutputStream(sock.getOutputStream())
                // CNXN：版本、最大数据、身份字符串
                send(CMD_CNXN, VERSION, MAX_DATA, "host::TvSwitch".toByteArray(Charsets.UTF_8))
                var attempts = 0
                while (attempts < 8) {
                    attempts++
                    when (val msg = readMsg()) {
                        else -> {
                            if (msg.cmd == CMD_CNXN) {
                                connected = true
                                return@withContext true
                            }
                            if (msg.cmd == CMD_AUTH) {
                                when (msg.arg0) {
                                    AUTH_TOKEN -> {
                                        val sig = keyStore.sign(keys.privateKey, msg.data)
                                        send(CMD_AUTH, AUTH_SIGNATURE, 0, sig)
                                    }
                                    else -> {
                                        // 密钥未被电视信任时，发送公钥，电视会弹出“允许调试？”提示
                                        send(
                                            CMD_AUTH, AUTH_RSA_PUBLIC, 0,
                                            keys.publicKeyString.toByteArray(Charsets.UTF_8)
                                        )
                                    }
                                }
                            }
                            if (msg.cmd == CMD_CLSE) {
                                lastError = "电视关闭了连接"
                                return@withContext false
                            }
                        }
                    }
                }
                lastError = "认证超时：请在电视上点“允许调试”，并核对指纹"
                false
            } catch (e: Exception) {
                lastError = e.message ?: "连接失败"
                disconnectQuietly()
                false
            }
        }

    /** 打开一个流并执行一条 shell 命令，返回全部输出；超时或关闭即返回已收到部分。 */
    suspend fun shell(command: String, timeoutMs: Int = 15000): String =
        withContext(Dispatchers.IO) {
            if (!connected) return@withContext ""
            val localId = nextLocalId.getAndIncrement()
            send(CMD_OPEN, localId, 0, "shell:$command".toByteArray(Charsets.UTF_8))
            var remoteId = -1
            val out = StringBuilder()
            val deadline = System.currentTimeMillis() + timeoutMs
            try {
                while (System.currentTimeMillis() < deadline) {
                    val msg = readMsg()
                    when (msg.cmd) {
                        CMD_OKAY -> remoteId = msg.arg1
                        CMD_WRTE -> {
                            out.append(String(msg.data, Charsets.UTF_8))
                            send(CMD_OKAY, localId, msg.arg1)
                        }
                        CMD_CLSE -> break
                    }
                }
            } catch (e: Exception) {
                // 超时或断开都按已有输出返回，不抛给界面
            }
            if (remoteId >= 0) {
                try { send(CMD_CLSE, localId, remoteId) } catch (_: Exception) {}
            }
            out.toString()
        }

    /** 打开 sync 流并推送一个文件；返回是否成功。sync 子命令均为 ASCII + 长度前缀。 */
    suspend fun pushFile(localBytes: ByteArray, remotePath: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!connected) return@withContext false
            try {
                val localId = nextLocalId.getAndIncrement()
                send(CMD_OPEN, localId, 0, "sync:".toByteArray(Charsets.UTF_8))
                var remoteId = -1
                val deadline = System.currentTimeMillis() + 60000
                // 等待 OKAY
                while (System.currentTimeMillis() < deadline) {
                    val m = readMsg()
                    if (m.cmd == CMD_OKAY) { remoteId = m.arg1; break }
                    if (m.cmd == CMD_CLSE) return@withContext false
                }
                if (remoteId < 0) return@withContext false
                fun syncWrite(payload: ByteArray) {
                    send(CMD_WRTE, localId, remoteId, payload)
                    // 等传输层 OKAY；期间若收到 sync 层 WRTE，先检查是否 FAIL 再继续等 OKAY
                    while (true) {
                        val m = readMsg()
                        if (m.cmd == CMD_CLSE) throw IllegalStateException("sync 关闭")
                        if (m.cmd == CMD_OKAY) break
                        if (m.cmd == CMD_WRTE) {
                            val text = String(m.data, Charsets.UTF_8)
                            send(CMD_OKAY, localId, remoteId)
                            if (text.startsWith("FAIL")) {
                                throw IllegalStateException(text)
                            }
                        }
                    }
                }
                fun syncHeader(id: String, size: Int): ByteArray {
                    val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                    bb.put(id.toByteArray(Charsets.UTF_8))
                    bb.putInt(size)
                    return bb.array()
                }
                val sendReq = syncHeader("SEND", remotePath.length + 5) +
                    remotePath.toByteArray(Charsets.UTF_8) + ",0644".toByteArray(Charsets.UTF_8)
                syncWrite(sendReq)
                var offset = 0
                while (offset < localBytes.size) {
                    val chunk = localBytes.copyOfRange(
                        offset, minOf(offset + 65536, localBytes.size)
                    )
                    syncWrite(syncHeader("DATA", chunk.size) + chunk)
                    offset += chunk.size
                }
                val now = (System.currentTimeMillis() / 1000).toInt()
                syncWrite(syncHeader("DONE", now))
                syncWrite("QUIT".toByteArray(Charsets.UTF_8) + ByteBuffer.allocate(4)
                    .order(ByteOrder.LITTLE_ENDIAN).putInt(0).array())
                try { send(CMD_CLSE, localId, remoteId) } catch (_: Exception) {}
                true
            } catch (e: Exception) {
                lastError = e.message ?: "传文件失败"
                false
            }
        }

    fun disconnectQuietly() {
        connected = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        input = null
        output = null
    }

    fun disconnect() = disconnectQuietly()
}
