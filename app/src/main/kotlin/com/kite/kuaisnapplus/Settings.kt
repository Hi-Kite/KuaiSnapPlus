package com.kite.kuaisnapplus

import android.content.Context
import android.content.SharedPreferences

/**
 * 单个开关设置项。
 *
 * @param key 存储键，同时也是 Hook 侧读取的键名，改动会导致设置失效
 */
data class SwitchItem(
    val key: String,
    val title: String,
    val summary: String,
    val default: Boolean,
)

/**
 * 下拉选择项（仅模块 App 使用，Hook 侧不读取）。
 *
 * @param entries 界面上显示的名称
 * @param values 与 entries 一一对应的存储值
 */
data class DropdownItem(
    val key: String,
    val title: String,
    val summary: String,
    val entries: List<String>,
    val values: List<String>,
    val default: String,
) {
    /** 把存储值换算成下拉框需要的下标 */
    fun indexOf(stored: String?): Int =
        values.indexOf(stored ?: default).takeIf { it >= 0 } ?: 0
}

/** 一组设置，界面上渲染成一张卡片 */
data class SettingsGroup(
    val title: String,
    val switches: List<SwitchItem> = emptyList(),
    val dropdowns: List<DropdownItem> = emptyList(),
)

/**
 * 设置项清单。
 *
 * 开关项的键名与 Hook 侧（xposed.java）一一对应，Hook 通过
 * XSharedPreferences 读取同一份 SharedPreferences；
 * 下拉项只影响模块 App 自身的外观。
 */
object SettingsCatalog {

    /** 与 Hook 侧共用的 SharedPreferences 文件名 */
    const val PREFS_NAME = "KuaiSnap_Settings"

    // ---- 外观相关（模块 App 自用）----

    const val KEY_COLOR_MODE = "color_mode"
    const val KEY_PALETTE_STYLE = "palette_style"
    const val KEY_KEY_COLOR = "key_color"

    const val COLOR_MODE_SYSTEM = "system"
    const val COLOR_MODE_LIGHT = "light"
    const val COLOR_MODE_DARK = "dark"
    const val COLOR_MODE_DYNAMIC = "dynamic"

    const val PALETTE_STYLE_DEFAULT = "tonal_spot"
    const val KEY_COLOR_DEFAULT = "default"

    val colorModeItem = DropdownItem(
        key = KEY_COLOR_MODE,
        title = "配色模式",
        summary = "「跟随系统」是 Miuix 内置的 MIUI / HyperOS 观感；「动态取色」改用 Material 3 调色盘",
        entries = listOf("跟随系统", "浅色", "深色", "动态取色"),
        values = listOf(
            COLOR_MODE_SYSTEM, COLOR_MODE_LIGHT, COLOR_MODE_DARK, COLOR_MODE_DYNAMIC,
        ),
        default = COLOR_MODE_SYSTEM,
    )

    /**
     * Material 3 调色盘（palette style）。
     *
     * 对应 material-color-utilities 的 9 种调色盘算法，同一个种子色经不同算法
     * 会得到明度分布、饱和度差异很大的整套配色。仅在「动态取色」模式下生效。
     */
    val paletteStyleItem = DropdownItem(
        key = KEY_PALETTE_STYLE,
        title = "调色盘",
        summary = "仅「动态取色」模式生效；同一主题色在不同调色盘下配色差异明显",
        entries = listOf(
            "标准", "中性", "鲜艳", "表现力", "彩虹", "果缤纷", "单色", "忠实", "内容",
        ),
        values = listOf(
            PALETTE_STYLE_DEFAULT, "neutral", "vibrant", "expressive", "rainbow",
            "fruit_salad", "monochrome", "fidelity", "content",
        ),
        default = PALETTE_STYLE_DEFAULT,
    )

    val keyColorItem = DropdownItem(
        key = KEY_KEY_COLOR,
        title = "主题色",
        summary = "仅「动态取色」模式生效；选「默认」则从系统壁纸取色（需 Android 12+）",
        entries = listOf("默认", "蓝", "靛蓝", "紫", "粉", "红", "橙", "黄", "绿", "青"),
        values = listOf(
            KEY_COLOR_DEFAULT, "blue", "indigo", "purple", "pink",
            "red", "orange", "yellow", "green", "teal",
        ),
        default = KEY_COLOR_DEFAULT,
    )

    val groups: List<SettingsGroup> = listOf(
        SettingsGroup(
            "外观",
            dropdowns = listOf(colorModeItem, paletteStyleItem, keyColorItem),
        ),
        SettingsGroup(
            "会员与内容",
            switches = listOf(
                SwitchItem("enable_vip", "解锁会员", "模拟 VIP 状态，解锁本地会员及部分功能", true),
                SwitchItem("enable_hd", "解锁高清内容", "解锁解析图片高清内容查看", true),
                SwitchItem("enable_rotate", "解锁横屏旋转", "解锁解析页面横屏旋转功能", true),
                SwitchItem("enable_vip_badge", "会员金标", "显示会员金标", true),
                SwitchItem(
                    "enable_video_explanation", "解锁讲解视频（实验）",
                    "延长讲解视频观看时间，不能真正解锁视频", false
                ),
            ),
        ),
        SettingsGroup(
            "图片与截屏",
            switches = listOf(
                SwitchItem("enable_image_decrypt", "图片保存", "绕过限制，保存解析页图片到相册", true),
                SwitchItem(
                    "remove_watermark", "无水印查看",
                    "仅去除网页浏览图片时的平铺水印，不影响已保存的图片", true
                ),
                SwitchItem("enable_screen_capture", "去除截屏限制", "去除截屏与录屏限制", true),
            ),
        ),
        SettingsGroup(
            "广告与界面",
            switches = listOf(
                SwitchItem("enable_ad_block", "纯净快对", "拦截广告请求", true),
                SwitchItem("block_vip_badge", "本大爷是 VIP", "去除 VIP 专属功能右上角角标", true),
                SwitchItem("block_new_user_banner", "我不是新人", "屏蔽「我的」页面顶部新人优惠广告", false),
                SwitchItem("remove_vip_banner", "去除会员 Banner", "屏蔽主页的会员 Banner", false),
                SwitchItem("block_notice_bar", "屏蔽提示", "去除解析页上方的「勤动脑，多思考」横条", false),
                SwitchItem("block_red_packet", "红包走开", "去除「我的」页面红包推广", false),
            ),
        ),
        SettingsGroup(
            "其他",
            switches = listOf(
                SwitchItem("block_collection_dialog", "不要收藏", "屏蔽退出解析页时的收藏弹窗", false),
                SwitchItem("enable_sensor_block", "禁用传感器", "禁用陀螺仪和加速度传感器", false),
                SwitchItem("block_startup_message", "屏蔽启动提示", "屏蔽软件启动时的提示", false),
            ),
        ),
        SettingsGroup(
            "模块",
            switches = listOf(
                SwitchItem("auto_update_enabled", "自动检查更新", "启动时检查模块新版本", true),
                SwitchItem("hide_app_icon", "隐藏桌面图标", "隐藏后可从 LSPosed 管理器进入本模块", false),
            ),
        ),
    )

    /** 全部开关项，用于批量读取初值 */
    val allSwitches: List<SwitchItem> = groups.flatMap { it.switches }

    /** 全部下拉项 */
    val allDropdowns: List<DropdownItem> = groups.flatMap { it.dropdowns }
}

/**
 * 设置读写。
 *
 * 写入时优先使用 `MODE_WORLD_READABLE`，以便 Hook 通过 XSharedPreferences
 * 读取；在模块未被 LSPosed 激活时该模式会抛 SecurityException，此时回退到私有模式，
 * 保证模块 App 自身始终可用。
 */
class SettingsStore(private val context: Context) {

    private val prefs: SharedPreferences = openPrefs()

    @Suppress("DEPRECATION")
    private fun openPrefs(): SharedPreferences =
        try {
            context.getSharedPreferences(SettingsCatalog.PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (_: SecurityException) {
            // 模块未激活或不支持新版 XSharedPreferences
            context.getSharedPreferences(SettingsCatalog.PREFS_NAME, Context.MODE_PRIVATE)
        }

    fun readSwitches(): Map<String, Boolean> =
        SettingsCatalog.allSwitches.associate { it.key to prefs.getBoolean(it.key, it.default) }

    fun readDropdowns(): Map<String, String> =
        SettingsCatalog.allDropdowns.associate { it.key to prefs.getString(it.key, it.default).orEmpty() }

    /** 首次运行时写入全部默认值，保证 Hook 侧读到的状态与界面一致 */
    fun ensureDefaults() {
        val editor = prefs.edit()
        SettingsCatalog.allSwitches.forEach { item ->
            if (!prefs.contains(item.key)) {
                editor.putBoolean(item.key, item.default)
            }
        }
        SettingsCatalog.allDropdowns.forEach { item ->
            if (!prefs.contains(item.key)) {
                editor.putString(item.key, item.default)
            }
        }
        editor.apply()
    }

    fun set(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun set(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
