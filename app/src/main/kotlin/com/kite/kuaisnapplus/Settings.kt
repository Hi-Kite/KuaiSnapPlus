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

/** 一组设置，界面上渲染成一张卡片 */
data class SettingsGroup(
    val title: String,
    val items: List<SwitchItem>,
)

/**
 * 设置项清单。
 *
 * 这些键名与 Hook 侧（xposed.java）一一对应，Hook 通过
 * XSharedPreferences 读取同一份 SharedPreferences。
 */
object SettingsCatalog {

    /** 与 Hook 侧共用的 SharedPreferences 文件名 */
    const val PREFS_NAME = "KuaiSnap_Settings"

    val groups: List<SettingsGroup> = listOf(
        SettingsGroup(
            "会员与内容",
            listOf(
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
            listOf(
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
            listOf(
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
            listOf(
                SwitchItem("block_collection_dialog", "不要收藏", "屏蔽退出解析页时的收藏弹窗", false),
                SwitchItem("enable_sensor_block", "禁用传感器", "禁用陀螺仪和加速度传感器", false),
                SwitchItem("block_startup_message", "屏蔽启动提示", "屏蔽软件启动时的提示", false),
            ),
        ),
        SettingsGroup(
            "模块",
            listOf(
                SwitchItem("auto_update_enabled", "自动检查更新", "启动时检查模块新版本", true),
                SwitchItem("hide_app_icon", "隐藏桌面图标", "隐藏后可从 LSPosed 管理器进入本模块", false),
            ),
        ),
    )

    /** 全部设置项，用于批量读取初值 */
    val allItems: List<SwitchItem> = groups.flatMap { it.items }
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

    private fun openPrefs(): SharedPreferences =
        try {
            context.getSharedPreferences(SettingsCatalog.PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (_: SecurityException) {
            // 模块未激活或不支持新版 XSharedPreferences
            context.getSharedPreferences(SettingsCatalog.PREFS_NAME, Context.MODE_PRIVATE)
        }

    fun readAll(): Map<String, Boolean> =
        SettingsCatalog.allItems.associate { it.key to prefs.getBoolean(it.key, it.default) }

    /** 首次运行时写入全部默认值，保证 Hook 侧读到的状态与界面一致 */
    fun ensureDefaults() {
        val editor = prefs.edit()
        SettingsCatalog.allItems.forEach { item ->
            if (!prefs.contains(item.key)) {
                editor.putBoolean(item.key, item.default)
            }
        }
        editor.apply()
    }

    fun set(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}
