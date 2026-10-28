package com.assan.finance.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.assan.finance.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Update check for the APK shared on WhatsApp / Google Drive (not installed from the Play Store, so
 * Play's in-app update cannot see it).
 *
 * The latest version is published in docs/version.json on the project's GitHub Pages site. When its
 * "versionCode" is higher than this app's, a dialog offers "Update", which opens the download link.
 * To release an update: upload the new APK to the same Google Drive file ("Manage versions"), then raise
 * versionCode / versionName in docs/version.json and push it.
 */
object SideloadUpdateChecker {

    private const val TAG = "SideloadUpdate"
    const val VERSION_URL = "https://nrsp-it-isbd.github.io/AsaanFinanceApp/version.json"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /** Asked once per app start, so "Later" does not bother the user again until the next start. */
    private var shownThisSession = false

    fun check(activity: Activity) {
        if (shownThisSession) return
        CoroutineScope(Dispatchers.IO).launch {
            val info = try {
                // query string avoids an old cached copy of the file
                val request = Request.Builder().url("$VERSION_URL?t=${System.currentTimeMillis()}").build()
                client.newCall(request).execute().use { r ->
                    if (!r.isSuccessful) null else JSONObject(r.body?.string() ?: "")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Update check skipped: ${e.message}")
                null
            } ?: return@launch

            val latest = info.optInt("versionCode", 0)
            val url = info.optString("downloadUrl", "")
            if (latest <= BuildConfig.VERSION_CODE || url.isBlank()) return@launch

            withContext(Dispatchers.Main) {
                if (activity.isFinishing || activity.isDestroyed || shownThisSession) return@withContext
                shownThisSession = true
                show(activity, info, url)
            }
        }
    }

    private fun show(activity: Activity, info: JSONObject, url: String) {
        val isUrdu = LocaleHelper.getLanguage(activity) == "ur"
        val force = info.optBoolean("force", false)
        val versionName = info.optString("versionName", "")
        val title = if (isUrdu) "نیا ورژن دستیاب ہے" else "New version available"
        val message = info.optString(if (isUrdu) "message_ur" else "message_en", "").ifBlank {
            if (isUrdu) "آسان فنانس کا نیا ورژن $versionName آ گیا ہے۔ اپڈیٹ کریں پر ٹیپ کریں، فائل ڈاؤن لوڈ کر کے انسٹال کریں۔ آپ کا اکاؤنٹ اور ڈیٹا محفوظ رہے گا۔"
            else "Asaan Finance $versionName is available. Tap Update, download the file and install it. Your account and data stay safe."
        }
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(!force)
            .setPositiveButton(if (isUrdu) "اپڈیٹ کریں" else "Update") { _, _ ->
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Log.w(TAG, "Could not open update link: ${e.message}")
                }
                // a required update keeps the app closed until it is installed
                if (force) activity.finishAffinity()
            }
        if (!force) builder.setNegativeButton(if (isUrdu) "بعد میں" else "Later", null)
        builder.show()
    }
}
