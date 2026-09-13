package com.kite.kuaisnapplus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.kite.kuaisnapplus.SettingsCatalog
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * 配色模式 → Miuix 的 ColorSchemeMode。
 *
 * 默认的 `System` 使用 **Miuix 内置配色**（MIUI / HyperOS 风格：主色 #3482FF、中性灰底），
 * 这是本项目的默认观感，不涉及 Material 3。
 *
 * `Monet*` 系列是 **Material 3 动态取色**（底层 material-color-utilities），
 * 只有用户显式选择「动态取色」时才会启用。
 */
private fun colorModeOf(value: String): ColorSchemeMode = when (value) {
    SettingsCatalog.COLOR_MODE_LIGHT -> ColorSchemeMode.Light
    SettingsCatalog.COLOR_MODE_DARK -> ColorSchemeMode.Dark
    SettingsCatalog.COLOR_MODE_DYNAMIC -> ColorSchemeMode.MonetSystem
    else -> ColorSchemeMode.System
}

/** 调色盘 → Material 3 的 palette style（仅动态取色模式生效） */
private fun paletteStyleOf(value: String): ThemePaletteStyle = when (value) {
    "neutral" -> ThemePaletteStyle.Neutral
    "vibrant" -> ThemePaletteStyle.Vibrant
    "expressive" -> ThemePaletteStyle.Expressive
    "rainbow" -> ThemePaletteStyle.Rainbow
    "fruit_salad" -> ThemePaletteStyle.FruitSalad
    "monochrome" -> ThemePaletteStyle.Monochrome
    "fidelity" -> ThemePaletteStyle.Fidelity
    "content" -> ThemePaletteStyle.Content
    else -> ThemePaletteStyle.TonalSpot
}

/**
 * 主题色种子 → Color。
 *
 * 返回 null 表示「默认」：此时动态取色从系统壁纸提取种子色（需要 Android 12+）。
 * 返回具体颜色时由该色生成整套调色盘，与壁纸无关。
 */
private fun keyColorOf(value: String): Color? = when (value) {
    "blue" -> Color(0xFF3482FF)
    "indigo" -> Color(0xFF3F51B5)
    "purple" -> Color(0xFF7C4DFF)
    "pink" -> Color(0xFFE91E63)
    "red" -> Color(0xFFF44336)
    "orange" -> Color(0xFFFF9800)
    "yellow" -> Color(0xFFFFC107)
    "green" -> Color(0xFF4CAF50)
    "teal" -> Color(0xFF009688)
    else -> null
}

/**
 * 应用主题。
 *
 * @param colorMode 见 [SettingsCatalog.colorModeItem]
 * @param paletteStyle 见 [SettingsCatalog.paletteStyleItem]
 * @param keyColor 见 [SettingsCatalog.keyColorItem]
 */
@Composable
fun KuaiSnapPlusTheme(
    colorMode: String,
    paletteStyle: String,
    keyColor: String,
    content: @Composable () -> Unit,
) {
    val controller = remember(colorMode, paletteStyle, keyColor) {
        ThemeController(
            colorSchemeMode = colorModeOf(colorMode),
            keyColor = keyColorOf(keyColor),
            paletteStyle = paletteStyleOf(paletteStyle),
        )
    }
    MiuixTheme(controller = controller, content = content)
}
