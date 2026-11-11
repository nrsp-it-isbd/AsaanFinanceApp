package com.assan.finance.utils

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import com.assan.finance.BuildConfig
import com.assan.finance.R
import com.google.android.material.button.MaterialButton
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
 * The latest version is published in docs/version.json on the project's GitHub Pages site:
 *   versionCode / versionName   the newest APK on the Google Drive link
 *   downloadUrl                 the Google Drive link
 *   whats_new_en / whats_new_ur "What's new" lines shown in the dialog
 *   message_en / message_ur     optional text under the title
 *   force                       true = no "Later" (the app cannot be used until it is updated)
 * To release an update: upload the new APK to the same Google Drive file ("Manage versions"), then raise
 * versionCode / versionName in docs/version.json and push it.
 *
 * The dialog is not shown again and again: "Later" hides it for 12 hours for that version, and "Update"
 * for 1 hour (the user leaves for Google Drive and the installer, then comes back to the app).
 */
object SideloadUpdateChecker {

    private const val TAG = "SideloadUpdate"
    const val VERSION_URL = "https://nrsp-it-isbd.github.io/AsaanFinanceApp/version.json"

    private const val PREFS = "sideload_update_prefs"
    private const val KEY_SNOOZE_VERSION = "snooze_version"
    private const val KEY_SNOOZE_UNTIL = "snooze_until"
    private const val LATER_MS = 12 * 60 * 60 * 1000L
    private const val AFTER_UPDATE_TAP_MS = 60 * 60 * 1000L

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private var dialog: Dialog? = null

    fun check(activity: Activity) {
        if (dialog?.isShowing == true) return
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
            val force = info.optBoolean("force", false)
            if (!force && isSnoozed(activity, latest)) return@launch

            withContext(Dispatchers.Main) {
                if (activity.isFinishing || activity.isDestroyed || dialog?.isShowing == true) return@withContext
                show(activity, info, latest, url, force)
            }
        }
    }

    private fun isSnoozed(context: Context, versionCode: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SNOOZE_VERSION, -1) == versionCode &&
                System.currentTimeMillis() < prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
    }

    private fun snooze(context: Context, versionCode: Int, forMs: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_SNOOZE_VERSION, versionCode)
            .putLong(KEY_SNOOZE_UNTIL, System.currentTimeMillis() + forMs)
            .apply()
    }

    private fun show(activity: Activity, info: JSONObject, versionCode: Int, url: String, force: Boolean) {
        val isUrdu = LocaleHelper.getLanguage(activity) == "ur"
        val versionName = info.optString("versionName", "")

        val d = Dialog(activity)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(R.layout.dialog_app_update)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        d.setCancelable(!force)
        d.setCanceledOnTouchOutside(false)

        d.findViewById<TextView>(R.id.tvUpdateTitle).text =
            if (isUrdu) "نیا اپڈیٹ دستیاب ہے" else "New Update Available"
        d.findViewById<TextView>(R.id.tvUpdateVersion).text =
            if (isUrdu) "ورژن $versionName" else "Version $versionName"
        d.findViewById<TextView>(R.id.tvUpdateMessage).text =
            info.optString(if (isUrdu) "message_ur" else "message_en", "").ifBlank {
                if (isUrdu) "ایپ کا نیا ورژن تیار ہے۔ تازہ ترین درستگیوں اور سہولیات کے لیے اپڈیٹ کریں۔ آپ کا اکاؤنٹ اور ڈیٹا محفوظ رہے گا۔"
                else "A new version of the app is ready. Please update to get the latest fixes and features. Your account and data stay safe."
            }

        // "What's new": list in version.json, so it changes without a new app
        val notesArray = info.optJSONArray(if (isUrdu) "whats_new_ur" else "whats_new_en")
            ?: info.optJSONArray("whats_new_en")
        val notes = (0 until (notesArray?.length() ?: 0)).mapNotNull { i ->
            notesArray?.optString(i)?.trim()?.takeIf { it.isNotEmpty() }
        }
        if (notes.isNotEmpty()) {
            d.findViewById<View>(R.id.svUpdateNotes).visibility = View.VISIBLE
            d.findViewById<TextView>(R.id.tvWhatsNewTitle).text = if (isUrdu) "نیا کیا ہے" else "What's new"
            d.findViewById<TextView>(R.id.tvUpdateNotes).text = notes.joinToString("\n") { "•  $it" }
        }

        d.findViewById<MaterialButton>(R.id.btnUpdateNow).apply {
            text = if (isUrdu) "ابھی اپڈیٹ کریں" else "Update Now"
            setOnClickListener {
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Log.w(TAG, "Could not open update link: ${e.message}")
                }
                if (force) {
                    // a required update keeps the app closed until it is installed
                    d.dismiss()
                    activity.finishAffinity()
                } else {
                    snooze(activity, versionCode, AFTER_UPDATE_TAP_MS)
                    d.dismiss()
                }
            }
        }
        d.findViewById<MaterialButton>(R.id.btnUpdateLater).apply {
            if (force) {
                visibility = View.GONE
            } else {
                text = if (isUrdu) "بعد میں" else "Later"
                setOnClickListener {
                    snooze(activity, versionCode, LATER_MS)
                    d.dismiss()
                }
            }
        }
        // back button also counts as "Later"
        if (!force) d.setOnCancelListener { snooze(activity, versionCode, LATER_MS) }

        // soft pulsing ring around the icon
        val ring = d.findViewById<View>(R.id.vUpdateRing)
        val pulse = ObjectAnimator.ofPropertyValuesHolder(
            ring,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.85f, 1.05f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.85f, 1.05f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.55f)
        ).apply {
            duration = 1100
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        d.setOnShowListener { pulse.start() }
        d.setOnDismissListener {
            pulse.cancel()
            if (dialog === d) dialog = null
        }

        dialog = d
        d.show()
    }
}
