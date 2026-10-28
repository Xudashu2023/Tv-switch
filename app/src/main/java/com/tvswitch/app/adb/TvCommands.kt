package com.tvswitch.app.adb

/** 电视端命令封装：全部走 ADB shell，命令本身是 Android 公开的 pm / dumpsys / input 等工具。 */
class TvCommands(private val adb: AdbClient) {

    data class AppInfo(val packageName: String, val label: String)

    suspend fun listPackages(thirdPartyOnly: Boolean): List<String> {
        val cmd = if (thirdPartyOnly) "pm list packages -3" else "pm list packages"
        return adb.shell(cmd).lines()
            .mapNotNull { it.removePrefix("package:").trim().takeIf { s -> s.isNotEmpty() } }
    }

    suspend fun deviceModel(): String {
        val model = adb.shell("getprop ro.product.model").trim()
        val brand = adb.shell("getprop ro.product.brand").trim()
        return listOf(brand, model).filter { it.isNotEmpty() }.joinToString(" ")
    }

    suspend fun overview(): Map<String, String> {
        val df = adb.shell("df /data | tail -n 1").trim()
        val mem = adb.shell("cat /proc/meminfo | head -n 3").trim()
        val temp = adb.shell("dumpsys battery | grep temperature").trim()
        val cpu = adb.shell("cat /proc/loadavg").trim()
        return mapOf("存储" to df, "内存" to mem, "电池温度" to temp, "负载" to cpu)
    }

    suspend fun disablePackage(pkg: String): Boolean {
        val out = adb.shell("pm disable-user --user 0 $pkg")
        return out.isBlank() || !out.contains("Error", ignoreCase = true)
    }

    suspend fun enablePackage(pkg: String): Boolean {
        val out = adb.shell("pm enable $pkg")
        return out.isBlank() || !out.contains("Error", ignoreCase = true)
    }

    /** 仅用于用户自己安装的应用；自带应用绝不走这条路。 */
    suspend fun uninstallUserPackage(pkg: String): Boolean {
        val out = adb.shell("pm uninstall $pkg")
        return out.contains("Success", ignoreCase = true)
    }

    suspend fun keyEvent(code: Int): String = adb.shell("input keyevent $code")

    suspend fun inputText(text: String): String {
        // input text 对空格与中文支持有限：空格转 %s，其余按原样传，失败时界面会提示
        val safe = text.replace(" ", "%s")
        return adb.shell("input text $safe")
    }

    suspend fun reboot(): String = adb.shell("reboot")

    suspend fun setAnimationScale(scale: String): String {
        return adb.shell(
            "settings put global window_animation_scale $scale;" +
                " settings put global transition_animation_scale $scale;" +
                " settings put global animator_duration_scale $scale"
        )
    }

    suspend fun setPrivateDns(hostname: String): String {
        return if (hostname.isBlank()) {
            adb.shell("settings put global private_dns_mode off")
        } else {
            adb.shell(
                "settings put global private_dns_mode hostname;" +
                    " settings put global private_dns_specifier $hostname"
            )
        }
    }

    suspend fun clearCaches(): String {
        // 不删除用户数据，只清缓存目录下的缓存文件，逐应用需 root 权限时会在输出中体现
        return adb.shell("pm trim-caches 1073741824")
    }

    suspend fun installApk(remotePath: String): Boolean {
        val out = adb.shell("pm install -r $remotePath")
        return out.contains("Success", ignoreCase = true)
    }

    suspend fun getInstalledVersion(pkg: String): String {
        return adb.shell("dumpsys package $pkg | grep versionName").trim()
    }
}
