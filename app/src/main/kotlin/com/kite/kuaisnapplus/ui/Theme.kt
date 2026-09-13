package com.kite.kuaisnapplus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 应用主题。
 *
 * 使用 Miuix 的 `MonetSystem` 模式：Android 12+ 从壁纸取色（Material You），
 * 低版本自动回退到 Miuix 默认配色，明暗跟随系统。
 */
@Composable
fun KuaiSnapPlusTheme(content: @Composable () -> Unit) {
    val controller = remember {
        ThemeController(colorSchemeMode = ColorSchemeMode.MonetSystem)
    }
    MiuixTheme(controller = controller, content = content)
}
