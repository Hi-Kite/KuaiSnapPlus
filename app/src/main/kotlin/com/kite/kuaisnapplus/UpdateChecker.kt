package com.kite.kuaisnapplus

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 更新清单 */
data class UpdateInfo(
    val versionName: String,
    val updateLog: String,
    val apkUrl: String,
)

/**
 * 更新检查。
 *
 * 清单地址指向本仓库的 update.json，与 release 页面配套使用。
 */
object UpdateChecker {

    private const val UPDATE_URL =
        "https://raw.githubusercontent.com/Hi-Kite/KuaiSnapPlus/main/update.json"

    /**
     * 拉取更新清单并与当前版本比较。
     * 网络异常、解析失败或已是最新版时返回 null。
     */
    suspend fun check(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(UPDATE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }

            val json = JSONObject(body)
            val remote = json.optString("versionName")
            val current = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName

            if (remote.isEmpty() || remote == current) {
                null
            } else {
                UpdateInfo(
                    versionName = remote,
                    updateLog = json.optString("updateLog"),
                    apkUrl = json.optString("apkUrl"),
                )
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
