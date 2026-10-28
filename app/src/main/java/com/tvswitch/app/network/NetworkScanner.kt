package com.tvswitch.app.network

import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** 同 Wi-Fi 下发现的设备：只代表该 IP 的端口是通的，不代表一定是电视。 */
data class DiscoveredTv(val ip: String, val port: Int)

object NetworkScanner {

    /** 本机在局域网里的 IPv4 地址；拿不到就返回 null，界面会提示手动输入。 */
    fun localIpv4(): String? {
        return try {
            val interfaces = java.util.Collections.list(NetworkInterface.getNetworkInterfaces())
            interfaces.flatMap { java.util.Collections.list(it.inetAddresses) }
                .firstOrNull { addr ->
                    !addr.isLoopbackAddress && addr.hostAddress?.contains('.') == true &&
                        !addr.isLinkLocalAddress
                }?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    fun subnetOf(ip: String): String? {
        val prefix = ip.substringBeforeLast('.', "")
        return if (prefix.isNotEmpty() && prefix != ip) "$prefix.0/24" else null
    }

    /**
     * 扫描本机所在 /24 子网的指定端口（默认 5555，网络调试端口）。
     * 只在局域网内建 TCP 连接，不出外网；单个地址超时 400 毫秒，32 路并发，
     * 254 个地址一般几秒内扫完。返回端口已开放的地址，按尾段排序。
     */
    suspend fun scan(
        port: Int,
        onProgress: (scanned: Int, total: Int) -> Unit
    ): List<DiscoveredTv> = coroutineScope {
        val ownIp = localIpv4() ?: return@coroutineScope emptyList()
        val prefix = ownIp.substringBeforeLast('.', "")
        if (prefix.isEmpty() || prefix == ownIp) return@coroutineScope emptyList()
        val total = 254
        val scanned = AtomicInteger(0)
        val jobs = (1..254).map { last ->
            async(Dispatchers.IO) {
                val ip = "$prefix.$last"
                try {
                    if (ip == ownIp) return@async null
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), 400)
                    }
                    DiscoveredTv(ip, port)
                } catch (e: Exception) {
                    null
                } finally {
                    val n = scanned.incrementAndGet()
                    if (n % 16 == 0 || n == total) onProgress(n, total)
                }
            }
        }
        jobs.mapNotNull { it.await() }
            .sortedBy { it.ip.substringAfterLast('.').toIntOrNull() ?: 0 }
    }
}
