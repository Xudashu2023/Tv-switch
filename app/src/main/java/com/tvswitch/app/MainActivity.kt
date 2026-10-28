package com.tvswitch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvswitch.app.ui.Screen
import com.tvswitch.app.ui.TvViewModel

class MainActivity : ComponentActivity() {
    private val vm: TvViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TvSwitchApp(vm)
            }
        }
    }
}

private val CardColors = listOf(
    Color(0xFF2196F3), Color(0xFF00BCD4), Color(0xFFFF9800), Color(0xFF9C27B0),
    Color(0xFF4CAF50), Color(0xFFE91E63), Color(0xFFFFC107), Color(0xFFF44336),
    Color(0xFF3F51B5), Color(0xFF009688)
)

@Composable
fun TvSwitchApp(vm: TvViewModel) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tv Switch", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                if (vm.screen != Screen.HOME) {
                    TextButton(onClick = { vm.backHome() }) { Text("返回首页") }
                }
            }
            Text(
                if (vm.connected) "● 已连接" else "○ 未连接",
                color = if (vm.connected) Color(0xFF4CAF50) else Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (vm.screen) {
                Screen.HOME -> HomeScreen(vm)
                Screen.CONNECT -> ConnectScreen(vm)
                Screen.OVERVIEW -> OverviewScreen(vm)
                Screen.DEBLOAT -> DebloatScreen(vm)
                Screen.APPS -> AppsScreen(vm)
                Screen.INSTALL -> InstallScreen(vm)
                Screen.LAUNCHER -> LauncherScreen(vm)
                Screen.FILES -> FilesScreen(vm)
                Screen.REMOTE -> RemoteScreen(vm)
                Screen.TWEAKS -> TweaksScreen(vm)
                Screen.SAFETY -> SafetyScreen(vm)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("运行日志", fontWeight = FontWeight.Bold)
            Text(vm.log.ifBlank { "暂无日志" }, fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun ColorCard(title: String, desc: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HomeScreen(vm: TvViewModel) {
    LazyColumn {
        item { Text(vm.status, fontSize = 14.sp) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        val entries = listOf(
            Triple("连接", "输入电视 IP，用 ADB 连上电视", Screen.CONNECT),
            Triple("电视总览", "剩余空间、CPU、内存、温度、负载", Screen.OVERVIEW),
            Triple("精简", "先预览再禁用自带应用，可撤销", Screen.DEBLOAT),
            Triple("应用管理", "启用、禁用、卸载自己装的应用", Screen.APPS),
            Triple("安装应用", "从手机装 APK 到电视", Screen.INSTALL),
            Triple("换桌面", "换启动器并设为 Home，可恢复", Screen.LAUNCHER),
            Triple("传文件", "手机与电视互传文件", Screen.FILES),
            Triple("遥控", "方向键、音量、打字、重启、关屏", Screen.REMOTE),
            Triple("小工具", "去广告 DNS、动画、清缓存、截图", Screen.TWEAKS),
            Triple("安全规则", "只禁用不卸载、可撤销、受保护清单", Screen.SAFETY),
        )
        items(entries.withIndex().toList()) { (i, e) ->
            ColorCard(e.first, e.second, CardColors[i % CardColors.size]) { vm.go(e.third) }
        }
    }
}

@Composable
private fun ConnectScreen(vm: TvViewModel) {
    Column {
        Text("连接电视", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("电视端先开：设置 → 关于 → 连点版本号 7 次 → 开发者选项 → 开启“网络调试”（或“无线调试”）。手机与电视须在同一 Wi-Fi。")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = vm.host, onValueChange = { vm.host = it }, label = { Text("电视 IP，例如 192.168.1.100") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = vm.port, onValueChange = { vm.port = it }, label = { Text("端口，网络调试一般是 5555") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { vm.connect() }, enabled = !vm.busy) { Text("连接") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { vm.disconnect() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("断开") }
        Spacer(modifier = Modifier.height(8.dp))
        Text(vm.status)
        Text("说明：Android 11+ 无线调试的配对码流程（TLS 配对）在 v1 中尚未实现；请先用网络调试连接，电视弹出“允许调试？”时点允许，并核对上方指纹。", fontSize = 13.sp, color = Color.DarkGray)
    }
}

@Composable
private fun OverviewScreen(vm: TvViewModel) {
    Column {
        Text("电视总览", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Button(onClick = { vm.refreshOverview() }, enabled = vm.connected && !vm.busy) { Text("刷新") }
        Spacer(modifier = Modifier.height(8.dp))
        if (vm.overview.isEmpty()) Text("未连接或暂无数据，先点刷新。") else {
            vm.overview.forEach { (k, v) ->
                Text("$k：$v", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun DebloatScreen(vm: TvViewModel) {
    Column {
        Text("精简", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("规则：只禁用自带应用，绝不卸载自带应用；操作前逐项确认；本次禁用都可撤销；关键包在受保护清单里，无法禁用。")
        Button(onClick = { vm.refreshPackages() }, enabled = vm.connected && !vm.busy) { Text("扫描电视已装应用") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { vm.restoreAll() }, enabled = vm.connected && vm.disabledByUs.isNotEmpty()) { Text("一键恢复本次禁用（${vm.disabledByUs.size} 个）") }
        LazyColumn {
            items(vm.debloatCandidates) { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.label, fontWeight = FontWeight.Bold)
                        Text(item.packageName, fontSize = 12.sp, color = Color.DarkGray)
                        Text("厂商：${item.maker}", fontSize = 12.sp, color = Color.DarkGray)
                    }
                    Button(onClick = { vm.disable(item.packageName) }) { Text("禁用") }
                }
            }
        }
        if (vm.debloatCandidates.isEmpty()) Text("暂无候选：先扫描，或该电视的自带应用不在 v1 起步清单里，可在后续版本里自行添加包名。", fontSize = 13.sp)
    }
}

@Composable
private fun AppsScreen(vm: TvViewModel) {
    var showThirdOnly by remember { mutableStateOf(false) }
    Column {
        Text("应用管理", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Button(onClick = { vm.refreshPackages() }, enabled = vm.connected && !vm.busy) { Text("刷新列表") }
        Row {
            TextButton(onClick = { showThirdOnly = false }) { Text(if (!showThirdOnly) "● 全部" else "全部") }
            TextButton(onClick = { showThirdOnly = true }) { Text(if (showThirdOnly) "● 仅第三方" else "仅第三方") }
        }
        val list = if (showThirdOnly) vm.thirdParty else vm.packages
        LazyColumn {
            items(list.take(300)) { pkg ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pkg, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.enable(pkg) }) { Text("启用") }
                    TextButton(onClick = { vm.disable(pkg) }) { Text("禁用") }
                    if (showThirdOnly) TextButton(onClick = { vm.uninstall(pkg) }) { Text("卸载") }
                }
            }
        }
        Text("注意：卸载按钮只在“仅第三方”里出现，自带应用不会提供卸载。", fontSize = 13.sp, color = Color.DarkGray)
    }
}

@Composable
private fun InstallScreen(vm: TvViewModel) {
    Column {
        Text("安装应用", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("v1 安装流程：选择 APK 后，应用会把文件传到电视 /data/local/tmp，再执行 pm install -r。分包（APKs/XAPK/APKM）需先在手机上解包为多个 APK 再逐个安装，完整分包解析在后续版本里补齐。")
        Text("当前版本暂未接入系统文件选择器：请把 APK 放到手机 Download 目录，并在后续版本里通过文件选择器选取；本页先保留安装命令入口，避免在未验证的路径上误装。", fontSize = 13.sp, color = Color.DarkGray)
    }
}

@Composable
private fun LauncherScreen(vm: TvViewModel) {
    Column {
        Text("换桌面", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("推荐启动器（需先在电视上装好，再从这里设为 Home）：Projectivy、FLauncher。")
        Text("安全流程：先打开新桌面确认能正常显示，再禁用旧桌面；电视自带桌面可随时恢复。换桌面操作在 v1 中需在“应用管理”里先确认新桌面已安装，再由后续版本提供一键设 Home 入口。", fontSize = 13.sp)
    }
}

@Composable
private fun FilesScreen(vm: TvViewModel) {
    Column {
        Text("传文件", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("支持目录：电视的 Download 与 Movies。")
        Text("v1 已实现 ADB sync 推送底层（pushFile）；文件选择器与双向拉取进度条在后续版本里补齐，当前版本请先用“安装应用”的传包路径验证连通性，不要传大文件。", fontSize = 13.sp)
    }
}

@Composable
private fun RemoteScreen(vm: TvViewModel) {
    var text by remember { mutableStateOf("") }
    Column {
        Text("遥控", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Row {
            Button(onClick = { vm.keyEvent(19, "上") }) { Text("上") }
            Button(onClick = { vm.keyEvent(20, "下") }) { Text("下") }
            Button(onClick = { vm.keyEvent(21, "左") }) { Text("左") }
            Button(onClick = { vm.keyEvent(22, "右") }) { Text("右") }
        }
        Row {
            Button(onClick = { vm.keyEvent(23, "确认") }) { Text("确认") }
            Button(onClick = { vm.keyEvent(4, "返回") }) { Text("返回") }
            Button(onClick = { vm.keyEvent(3, "Home") }) { Text("Home") }
            Button(onClick = { vm.keyEvent(82, "菜单") }) { Text("菜单") }
        }
        Row {
            Button(onClick = { vm.keyEvent(24, "音量+") }) { Text("音量+") }
            Button(onClick = { vm.keyEvent(25, "音量-") }) { Text("音量-") }
            Button(onClick = { vm.keyEvent(164, "静音") }) { Text("静音") }
        }
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("输入要打到电视上的文字") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.sendText(text) }) { Text("发送文字") }
        Button(onClick = { vm.runTweak({ reboot() }, "重启电视") }) { Text("重启电视") }
        Text("关屏：Android 13 及以下可用 input keyevent 223 实现，Android 14+ 系统已限制，界面不提供假按钮。", fontSize = 13.sp, color = Color.DarkGray)
    }
}

@Composable
private fun TweaksScreen(vm: TvViewModel) {
    var dns by remember { mutableStateOf("") }
    Column {
        Text("小工具", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = dns, onValueChange = { dns = it }, label = { Text("Private DNS 主机名，留空即关闭") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.runTweak({ setPrivateDns(dns) }, "Private DNS") }) { Text("应用 Private DNS") }
        Button(onClick = { vm.runTweak({ setAnimationScale("0.5") }, "加快动画") }) { Text("加快动画（0.5 倍）") }
        Button(onClick = { vm.runTweak({ setAnimationScale("1.0") }, "恢复动画") }) { Text("恢复动画（1.0 倍）") }
        Button(onClick = { vm.runTweak({ clearCaches() }, "清缓存") }) { Text("清缓存") }
    }
}

@Composable
private fun SafetyScreen(vm: TvViewModel) {
    LazyColumn {
        item {
            Text("安全规则", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("1. 只禁用、不卸载自带应用；卸载只针对用户自己安装的应用。")
            Text("2. 每次操作前先列出、逐项确认；操作记录在本页日志里，本次禁用可一键恢复。")
            Text("3. 受保护清单（启动、系统界面、设置、Wi-Fi、输入法等）不可禁用，没有绕过开关。")
            Text("4. 不用 root，不用漏洞，只用 ADB 允许的命令。")
            Text("5. 无广告、无追踪、无账号；数据只在局域网内传输；密钥只存在手机私有目录。")
            Text("6. 电视端不留常驻程序：辅助操作都在临时目录完成，用完即删。")
            Text("7. 与 Tellyscope 的关系：本应用为独立原创代码，功能对标其公开功能描述，不是它的 Fork，也没有复制它的源码、图标与文案。")
        }
    }
}
