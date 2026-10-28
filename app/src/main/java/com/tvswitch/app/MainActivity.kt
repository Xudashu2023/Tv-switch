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
private fun ActionRow(title: String, desc: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(desc, fontSize = 12.sp, color = Color.DarkGray)
            }
            Text("›", fontSize = 22.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
private fun HomeScreen(vm: TvViewModel) {
    LazyColumn {
        // 顶部：电视一览卡（Tellyscope 式：先看电视，再看操作）
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (vm.connected) (vm.deviceModel.ifBlank { "已连接的电视" }) else "未连接电视",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (vm.connected) "已连接：${vm.host}:${vm.port}" else "先在“添加电视”里扫一扫或手动添加",
                        fontSize = 13.sp, color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = { vm.go(Screen.CONNECT) }) {
                            Text(if (vm.connected) "换一台电视" else "添加电视")
                        }
                        Spacer(modifier = Modifier.padding(4.dp))
                        if (vm.connected) {
                            Button(onClick = { vm.go(Screen.OVERVIEW) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                                Text("看电视状态")
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("电视") }
        item { ActionRow("电视总览", "剩余空间、CPU、内存、温度、负载") { vm.go(Screen.OVERVIEW) } }
        item { ActionRow("遥控", "方向键、音量、打字、重启、关屏") { vm.go(Screen.REMOTE) } }
        item { SectionTitle("应用") }
        item { ActionRow("精简", "先预览再禁用自带应用，可撤销") { vm.go(Screen.DEBLOAT) } }
        item { ActionRow("应用管理", "启用、禁用、卸载自己装的应用") { vm.go(Screen.APPS) } }
        item { ActionRow("安装应用", "从手机装 APK 到电视") { vm.go(Screen.INSTALL) } }
        item { SectionTitle("传输与桌面") }
        item { ActionRow("传文件", "手机与电视互传文件") { vm.go(Screen.FILES) } }
        item { ActionRow("换桌面", "换启动器并设为 Home，可恢复") { vm.go(Screen.LAUNCHER) } }
        item { SectionTitle("其他") }
        item { ActionRow("小工具", "去广告 DNS、动画、清缓存、截图") { vm.go(Screen.TWEAKS) } }
        item { ActionRow("安全规则", "只禁用不卸载、可撤销、受保护清单") { vm.go(Screen.SAFETY) } }
    }
}

@Composable
private fun ConnectScreen(vm: TvViewModel) {
    LazyColumn {
        item {
            Text("添加电视", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            // 品牌选择：先选品牌，再按该品牌的菜单名去开调试
            Text("电视品牌", fontWeight = FontWeight.Bold)
            Row {
                Button(
                    onClick = { vm.brand = "海信 VIDDA" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vm.brand == "海信 VIDDA") Color(0xFF2196F3) else Color.Gray
                    )
                ) { Text("海信 VIDDA") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(
                    onClick = { vm.brand = "通用 Android TV" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vm.brand == "通用 Android TV") Color(0xFF2196F3) else Color.Gray
                    )
                ) { Text("通用 Android TV") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (vm.brand == "海信 VIDDA") {
                Text("海信 VIDDA 开启参考（不同固件菜单名略有出入）：设置 → 关于 → 连点“版本号”7 次开开发者选项 → 回到开发者选项 → 开启 ADB 调试与网络调试。手机与电视须在同一 Wi-Fi。", fontSize = 13.sp)
            } else {
                Text("通用开启参考：设置 → 关于 → 连点版本号 7 次 → 开发者选项 → 开启网络调试（或无线调试）。手机与电视须在同一 Wi-Fi。", fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 自动扫描：同 Wi-Fi 下找 5555 端口已开放的设备
            Text("同 Wi-Fi 自动扫描", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("只扫本机所在子网，不出局域网；扫到的是“5555 端口已开放”的设备，点连接后 ADB 会再验证是不是电视。", fontSize = 13.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = { vm.startScan() }, enabled = !vm.scanning && !vm.busy) { Text("开始扫描") }
                Spacer(modifier = Modifier.padding(4.dp))
                if (vm.scanning) {
                    Button(onClick = { vm.stopScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("停止") }
                }
            }
            if (vm.scanSubnet.isNotEmpty()) Text("本机子网：${vm.scanSubnet}", fontSize = 13.sp)
            if (vm.scanProgress.isNotEmpty()) Text(vm.scanProgress, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (!vm.scanning && vm.discovered.isEmpty() && vm.scanProgress.isEmpty()) {
                Text("还没扫描：点“开始扫描”，VIDDA 开好网络调试后一般几秒内出现。", fontSize = 13.sp, color = Color.DarkGray)
            }
        }
        items(vm.discovered) { tv ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tv.ip, fontWeight = FontWeight.Bold)
                    Text("端口 ${tv.port} 已开放 · ${vm.brand}", fontSize = 12.sp, color = Color.DarkGray)
                }
                Button(onClick = { vm.connectTo(tv) }, enabled = !vm.busy) { Text("连接") }
            }
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("手动添加", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = vm.host, onValueChange = { vm.host = it }, label = { Text("电视 IP，例如 192.168.1.100") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = vm.port, onValueChange = { vm.port = it }, label = { Text("端口，网络调试一般是 5555") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { vm.connect() }, enabled = !vm.busy) { Text("连接") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { vm.disconnect() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("断开") }
            Spacer(modifier = Modifier.height(8.dp))
            Text(vm.status)
            Text("说明：Android 11+ 无线调试的配对码流程（TLS 配对）在本版中尚未实现；请先用网络调试连接，电视弹出“允许调试？”时点允许，并核对上方指纹。扫描不到时，先确认 VIDDA 已开网络调试、与手机同一 Wi-Fi，再扫一次或手动添加。", fontSize = 13.sp, color = Color.DarkGray)
        }
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
        // 顶部先显示电视型号（Tellyscope 精简页式：型号在上，待禁用列表在下）
        Text(
            if (vm.deviceModel.isNotEmpty()) "电视：${vm.deviceModel}" else "未连接时看不到型号：连接后这里会显示电视品牌与型号。",
            fontSize = 13.sp, color = Color.DarkGray
        )
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
        // 方向盘：中间是确认，上下左右居中排（Tellyscope 式遥控布局）
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { vm.keyEvent(19, "上") }) { Text("上") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { vm.keyEvent(21, "左") }) { Text("左") }
            Button(onClick = { vm.keyEvent(23, "确认") }) { Text("确认") }
            Button(onClick = { vm.keyEvent(22, "右") }) { Text("右") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { vm.keyEvent(20, "下") }) { Text("下") }
        }
        Row {
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
        Row {
            Button(onClick = { vm.sendText(text) }) { Text("发送文字") }
            Button(onClick = { vm.keyEvent(67, "删除") }) { Text("删除") }
        }
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
