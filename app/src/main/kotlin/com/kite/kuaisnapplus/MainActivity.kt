package com.kite.kuaisnapplus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kite.kuaisnapplus.ui.KuaiSnapPlusTheme
import com.kite.kuaisnapplus.ui.SettingsScreen

/**
 * 模块主界面 —— 单页设置。
 *
 * 所有 Hook 开关集中在这一页，按功能分组为若干卡片。
 * 设置写入模块自身的 SharedPreferences（MODE_WORLD_READABLE），
 * Hook 侧通过 XSharedPreferences 读取同一份文件。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = SettingsStore(applicationContext)
        store.ensureDefaults()

        setContent {
            KuaiSnapPlusTheme {
                var agreed by rememberSaveable { mutableStateOf(isUserAgreed()) }
                var values by remember { mutableStateOf(store.readAll()) }
                var update by remember { mutableStateOf<UpdateInfo?>(null) }

                if (!agreed) {
                    AgreementDialog(
                        onAgree = {
                            markUserAgreed()
                            agreed = true
                        },
                        onReject = { finish() },
                    )
                } else {
                    LaunchedEffect(Unit) {
                        if (values["auto_update_enabled"] != false) {
                            update = UpdateChecker.check(this@MainActivity)
                        }
                    }

                    update?.let { info ->
                        UpdateDialog(
                            info = info,
                            onDismiss = { update = null },
                            onDownload = {
                                openUrl(info.apkUrl)
                                update = null
                            },
                        )
                    }

                    SettingsScreen(
                        values = values,
                        activated = ModuleStatus.activated,
                        onToggle = { item, checked ->
                            store.set(item.key, checked)
                            values = values + (item.key to checked)
                            if (item.key == KEY_HIDE_ICON) {
                                setLauncherIconVisible(this, !checked)
                            }
                        },
                    )
                }
            }
        }
    }

    private fun isUserAgreed(): Boolean =
        getSharedPreferences(MODULE_PREFS, MODE_PRIVATE).getBoolean(KEY_AGREED, false)

    private fun markUserAgreed() {
        getSharedPreferences(MODULE_PREFS, MODE_PRIVATE).edit().putBoolean(KEY_AGREED, true).apply()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val MODULE_PREFS = "module_sp"
        private const val KEY_AGREED = "user_agreed"
        private const val KEY_HIDE_ICON = "hide_app_icon"

        const val ALIAS_ACTIVITY = "com.kite.kuaisnapplus.LauncherAlias"

        /** 是否隐藏桌面图标（通过启用/禁用 activity-alias 实现） */
        fun setLauncherIconVisible(context: Context, visible: Boolean) {
            val target = if (visible) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                val pm = context.packageManager
                val component = ComponentName(context, ALIAS_ACTIVITY)
                if (pm.getComponentEnabledSetting(component) != target) {
                    pm.setComponentEnabledSetting(component, target, PackageManager.DONT_KILL_APP)
                }
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
private fun AgreementDialog(onAgree: () -> Unit, onReject: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("软件使用声明") },
        text = {
            Text(
                "本项目「快怼+」（KuaiSnapPlus）是原作者 JiGuro 的「快怼」（KuaiSnap）" +
                    "项目的 fork，由 Kite 在原项目基础上继续维护，沿用原项目的使用条款与免责声明。\n\n" +
                    "本软件仅供学习研究软件的设计思想与原理，严禁用于任何商业或非法目的，" +
                    "严禁用于任何违反当地法律法规的用途。用户需确保自身使用行为符合相关法律法规，" +
                    "一切法律责任由使用者自行承担。\n\n" +
                    "本软件完全免费，如果您是付费获得的，那么您受骗了。\n\n" +
                    "点击「我已阅读并同意」即表示您已完整阅读、理解并接受上述内容。"
            )
        },
        confirmButton = { TextButton(onClick = onAgree) { Text("我已阅读并同意") } },
        dismissButton = { TextButton(onClick = onReject) { Text("拒绝并退出") } },
    )
}

@Composable
private fun UpdateDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本 v${info.versionName}") },
        text = { Text(info.updateLog.ifEmpty { "有新版本可用" }) },
        confirmButton = { TextButton(onClick = onDownload) { Text("立即下载") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("以后再说") } },
    )
}
