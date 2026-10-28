package com.tvswitch.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvswitch.app.adb.AdbClient
import com.tvswitch.app.adb.AdbKeyStore
import com.tvswitch.app.adb.TvCommands
import com.tvswitch.app.data.DebloatLists
import com.tvswitch.app.network.DiscoveredTv
import com.tvswitch.app.network.NetworkScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class Screen {
    HOME, CONNECT, OVERVIEW, DEBLOAT, APPS, INSTALL, LAUNCHER, FILES, REMOTE, TWEAKS, SAFETY
}

class TvViewModel(app: Application) : AndroidViewModel(app) {
    private val keyStore = AdbKeyStore(app)
    private var adb: AdbClient? = null
    private var cmds: TvCommands? = null

    var screen by mutableStateOf(Screen.HOME)
        private set
    var status by mutableStateOf("未连接：先在“连接”里输入电视 IP")
        private set
    var connected by mutableStateOf(false)
        private set
    var host by mutableStateOf("")
    var port by mutableStateOf("5555")
    var log by mutableStateOf("")
        private set
    var packages by mutableStateOf<List<String>>(emptyList())
        private set
    var thirdParty by mutableStateOf<List<String>>(emptyList())
        private set
    var overview by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var debloatCandidates by mutableStateOf<List<com.tvswitch.app.data.DebloatItem>>(emptyList())
        private set
    var disabledByUs by mutableStateOf<List<String>>(emptyList())
        private set
    var busy by mutableStateOf(false)
        private set
    // v2：添加电视（品牌 + 自动扫描）
    var brand by mutableStateOf("海信 VIDDA")
    var deviceModel by mutableStateOf("")
        private set
    var scanning by mutableStateOf(false)
        private set
    var scanProgress by mutableStateOf("")
        private set
    var scanSubnet by mutableStateOf("")
        private set
    var discovered by mutableStateOf<List<DiscoveredTv>>(emptyList())
        private set
    private var scanJob: Job? = null

    fun go(s: Screen) { screen = s }
    fun backHome() { screen = Screen.HOME }

    private fun appendLog(text: String) {
        log = (log + "\n" + text).takeLast(4000)
    }

    fun connect() {
        val h = host.trim()
        val p = port.trim().toIntOrNull() ?: 5555
        if (h.isEmpty()) {
            status = "请先输入电视 IP"
            return
        }
        busy = true
        status = "正在连接 $h:$p …"
        viewModelScope.launch {
            try {
                val keys = keyStore.loadOrCreate()
                val client = AdbClient(keys, keyStore)
                val ok = client.connect(h, p)
                adb = client
                cmds = TvCommands(client)
                connected = ok
                status = if (ok) {
                    "已连接：$h:$p\n指纹（核对用）：${keyStore.fingerprint(keys.publicKeyString)}"
                } else {
                    "连接失败：${client.lastError}\n请确认电视与手机在同一 Wi-Fi，且电视已开启网络调试；首次连接请在电视上点“允许调试”。"
                }
                if (ok) {
                    appendLog("已连接 $h:$p")
                    try {
                        deviceModel = cmds?.deviceModel().orEmpty()
                    } catch (e: Exception) {
                        deviceModel = ""
                    }
                }
            } catch (e: Exception) {
                status = "连接失败：${e.message}"
            } finally {
                busy = false
            }
        }
    }

    fun disconnect() {
        adb?.disconnect()
        adb = null
        cmds = null
        connected = false
        deviceModel = ""
        status = "已断开"
    }

    fun startScan() {
        if (scanning) return
        scanning = true
        discovered = emptyList()
        scanProgress = "正在扫描…"
        scanJob = viewModelScope.launch {
            try {
                val ownIp = NetworkScanner.localIpv4()
                scanSubnet = if (ownIp != null) {
                    NetworkScanner.subnetOf(ownIp) ?: "未知子网"
                } else {
                    "拿不到本机局域网地址"
                }
                if (ownIp == null) {
                    scanProgress = "拿不到本机局域网地址，请用下方手动添加。"
                    return@launch
                }
                val port = port.trim().toIntOrNull() ?: 5555
                val found = NetworkScanner.scan(port) { s, t ->
                    scanProgress = "已扫 $s/$t"
                }
                discovered = found
                scanProgress = if (found.isEmpty()) {
                    "扫完，未发现开放 $port 端口的设备：请确认电视已开网络调试且与手机同一 Wi-Fi，再扫一次或手动添加。"
                } else {
                    "发现 ${found.size} 台开放 $port 端口的设备，点“连接”逐台验证。"
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    scanProgress = "已停止扫描"
                } else {
                    scanProgress = "扫描失败：${e.message}"
                }
            } finally {
                scanning = false
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun connectTo(tv: DiscoveredTv) {
        stopScan()
        host = tv.ip
        port = tv.port.toString()
        connect()
    }

    fun refreshPackages() {
        val c = cmds ?: return
        busy = true
        viewModelScope.launch {
            try {
                packages = c.listPackages(false)
                thirdParty = c.listPackages(true)
                debloatCandidates = DebloatLists.forInstalled(packages.toSet())
                appendLog("已刷新应用列表：共 ${packages.size} 个，第三方 ${thirdParty.size} 个")
            } finally { busy = false }
        }
    }

    fun refreshOverview() {
        val c = cmds ?: return
        busy = true
        viewModelScope.launch {
            try {
                overview = c.overview()
                appendLog("已刷新电视总览")
            } finally { busy = false }
        }
    }

    fun disable(pkg: String) {
        val c = cmds ?: return
        busy = true
        viewModelScope.launch {
            try {
                if (c.disablePackage(pkg)) {
                    disabledByUs = disabledByUs + pkg
                    appendLog("已禁用 $pkg（可撤销）")
                } else appendLog("禁用失败：$pkg")
            } finally { busy = false }
        }
    }

    fun enable(pkg: String) {
        val c = cmds ?: return
        busy = true
        viewModelScope.launch {
            try {
                if (c.enablePackage(pkg)) {
                    disabledByUs = disabledByUs - pkg
                    appendLog("已恢复 $pkg")
                } else appendLog("恢复失败：$pkg")
            } finally { busy = false }
        }
    }

    fun restoreAll() {
        val c = cmds ?: return
        busy = true
        viewModelScope.launch {
            try {
                for (pkg in disabledByUs.toList()) c.enablePackage(pkg)
                disabledByUs = emptyList()
                appendLog("已恢复本次全部禁用")
            } finally { busy = false }
        }
    }

    fun uninstall(pkg: String) {
        val c = cmds ?: return
        busy = true
        viewModelScope.launch {
            try {
                appendLog(if (c.uninstallUserPackage(pkg)) "已卸载 $pkg" else "卸载失败：$pkg")
                refreshPackages()
            } finally { busy = false }
        }
    }

    fun keyEvent(code: Int, label: String) {
        val c = cmds ?: return
        viewModelScope.launch {
            c.keyEvent(code)
            appendLog("遥控：$label")
        }
    }

    fun sendText(text: String) {
        val c = cmds ?: return
        viewModelScope.launch {
            c.inputText(text)
            appendLog("已发送文字")
        }
    }

    fun runTweak(action: suspend TvCommands.() -> String, label: String) {
        val c = cmds ?: return
        busy = true
        viewModelScope.launch {
            try {
                c.action()
                appendLog("小工具：$label 已执行")
            } catch (e: Exception) {
                appendLog("小工具失败：$label ${e.message}")
            } finally { busy = false }
        }
    }
}
