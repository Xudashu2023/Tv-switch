package com.tvswitch.app.data

/**
 * 精简清单（v1 起步清单，原创整理）。
 * 仅作“建议禁用”的参考：界面会先比对电视上实际已安装的包，
 * 用户逐项勾选确认后才执行 pm disable-user；不在清单里的包不会被自动处理。
 * 受保护清单不可禁用，且无任何绕过开关。
 */
data class DebloatItem(val packageName: String, val label: String, val maker: String)

object DebloatLists {
    /** 绝不可禁用的关键包：启动、系统界面、Wi-Fi、输入法、设置等。 */
    val protectedPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.launcher",
        "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin",
        "com.android.wifi",
        "com.android.phone",
        "com.android.providers.settings",
        "com.android.server.telecom",
    )

    val items = listOf(
        DebloatItem("com.google.android.tvrecommendations", "电视推荐", "通用"),
        DebloatItem("com.google.android.play.games", "Play 游戏", "通用"),
        DebloatItem("com.google.android.youtube.tv", "YouTube（电视版）", "通用"),
        DebloatItem("com.google.android.videos", "Google Play 影视", "通用"),
        DebloatItem("com.google.android.apps.tv.launcherx", "电视启动器推荐", "通用"),
        DebloatItem("com.xiaomi.mitv.service", "小米电视服务", "小米"),
        DebloatItem("com.xiaomi.tv.ad", "小米电视广告", "小米"),
        DebloatItem("com.xiaomi.mitv.shop", "小米电视商城", "小米"),
        DebloatItem("com.tcl.tv.tclhome", "TCL 桌面推荐", "TCL"),
        DebloatItem("com.tcl.android.tvads", "TCL 广告", "TCL"),
        DebloatItem("com.hisense.tv.service", "海信电视服务", "海信"),
        DebloatItem("com.hisense.adservice", "海信广告服务", "海信"),
        DebloatItem("com.sony.tv.sideview", "索尼 SideView", "索尼"),
        DebloatItem("com.sony.dtv.sonyselect", "索尼精选", "索尼"),
        DebloatItem("com.samsung.tv.plus", "三星 TV Plus", "三星"),
        DebloatItem("com.samsung.android.tvads", "三星广告", "三星"),
        DebloatItem("com.amazon.tv.launcher", "Fire TV 启动器推荐", "亚马逊"),
        DebloatItem("com.amazon.bueller.photos", "亚马逊照片", "亚马逊"),
    )

    fun forInstalled(installed: Set<String>): List<DebloatItem> {
        return items.filter { it.packageName in installed }
            .filter { it.packageName !in protectedPackages }
    }
}
