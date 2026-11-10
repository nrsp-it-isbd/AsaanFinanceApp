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
            if (isUrdu) "اپڈیٹ کریں پر ٹیپ کریں، فائل ڈاؤن لوڈ کر کے انسٹال کریں۔ آپ کا اکاؤنٹ اور ڈیٹا محفوظ رہے گا۔"
            else "Tap Update, download the file and install it. Your account and data stay safe."
        }
        // "What's new": list in version.json ("whats_new_en" / "whats_new_ur"), so it changes without a new app
        val items = info.optJSONArray(if (isUrdu) "whats_new_ur" else "whats_new_en")
            ?: info.optJSONArray("whats_new_en")

        val dialog = android.app.Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(com.assan.finance.R.layout.dialog_app_update)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(!force)

        dialog.findViewById<android.widget.TextView>(com.assan.finance.R.id.tvUpdateTitle).text = title
        dialog.findViewById<android.widget.TextView>(com.assan.finance.R.id.tvUpdateVersion).text =
            if (isUrdu) "ورژن $versionName" else "Version $versionName"
        dialog.findViewById<android.widget.TextView>(com.assan.finance.R.id.tvUpdateMessage).text = message

        val box = dialog.findViewById<android.view.View>(com.assan.finance.R.id.layoutWhatsNew)
        val list = dialog.findViewById<android.widget.LinearLayout>(com.assan.finance.R.id.listWhatsNew)
        if (items == null || items.length() == 0) {
            box.visibility = android.view.View.GONE
        } else {
            dialog.findViewById<android.widget.TextView>(com.assan.finance.R.id.tvWhatsNewTitle).text =
                if (isUrdu) "نیا کیا ہے" else "What's new"
            val teal = androidx.core.content.ContextCompat.getColor(activity, com.assan.finance.R.color.primary)
            val textColor = androidx.core.content.ContextCompat.getColor(activity, com.assan.finance.R.color.on_surface)
            val dp = activity.resources.displayMetrics.density
            for (i in 0 until items.length()) {
                val text = items.optString(i).trim()
                if (text.isEmpty()) continue
                val row = android.widget.TextView(activity)
                val line = android.text.SpannableString("✓  $text")
                line.setSpan(android.text.style.ForegroundColorSpan(teal), 0, 1, 0)
                line.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, 1, 0)
                row.text = line
                row.setTextColor(textColor)
                row.textSize = 14f
                row.textAlignment = android.view.View.TEXT_ALIGNMENT_VIEW_START
                row.setPadding(0, (4 * dp).toInt(), 0, (4 * dp).toInt())
                list.addView(row)
            }
            // keep the dialog on screen when the list is long
            val scroll = dialog.findViewById<android.view.View>(com.assan.finance.R.id.scrollWhatsNew)
            scroll.post {
                val max = (180 * dp).toInt()
                if (scroll.height > max) scroll.layoutParams = scroll.layoutParams.apply { height = max }
            }
        }

        val updateButton = dialog.findViewById<android.widget.Button>(com.assan.finance.R.id.btnUpdateNow)
        updateButton.text = if (isUrdu) "اپڈیٹ کریں" else "Update"
        updateButton.setOnClickListener {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Log.w(TAG, "Could not open update link: ${e.message}")
            }
            dialog.dismiss()
            // a required update keeps the app closed until it is installed
            if (force) activity.finishAffinity()
        }
        val later = dialog.findViewById<android.widget.TextView>(com.assan.finance.R.id.btnUpdateLater)
        if (force) {
            later.visibility = android.view.View.GONE
        } else {
            later.text = if (isUrdu) "بعد میں" else "Later"
            later.setOnClickListener { dialog.dismiss() }
        }
        dialog.show()
    }
}
