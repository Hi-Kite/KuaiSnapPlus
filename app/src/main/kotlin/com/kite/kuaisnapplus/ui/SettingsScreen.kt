package com.kite.kuaisnapplus.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kite.kuaisnapplus.SettingsCatalog
import com.kite.kuaisnapplus.SwitchItem
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 单页设置界面。
 *
 * 视觉参考 KernelSU Manager：卡片分组 + 组内「标题 / 副标题 / 开关」行，
 * 分组标题用 SmallTitle，组与组之间留白。
 */
@Composable
fun SettingsScreen(
    values: Map<String, Boolean>,
    activated: Boolean,
    onToggle: (SwitchItem, Boolean) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "快怼+",
                subtitle = "支持快对 6.77.0 / 7.7.0",
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            // 状态卡片
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    BasicComponent(
                        title = "模块状态",
                        summary = if (activated) {
                            "已激活 · 打开快对后设置生效"
                        } else {
                            "未激活 · 请在 LSPosed 中启用本模块并勾选作用域"
                        },
                    )
                }
            }

            // 设置分组
            SettingsCatalog.groups.forEach { group ->
                item { SmallTitle(group.title) }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        group.items.forEachIndexed { index, item ->
                            SwitchPreference(
                                checked = values[item.key] ?: item.default,
                                onCheckedChange = { onToggle(item, it) },
                                title = item.title,
                                summary = item.summary,
                            )
                            if (index != group.items.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
