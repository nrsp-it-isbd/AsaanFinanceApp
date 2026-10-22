package com.assan.finance.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.assan.finance.R
import com.assan.finance.utils.Constants
import com.assan.finance.utils.LocaleHelper
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * In-app User Guide: a hero header, a "3 steps to apply" strip, topic chips and one
 * expandable card per topic (screenshots, numbered steps and a tip). All text comes from
 * strings_user_guide.xml (English) and values-ur/strings_user_guide.xml (Urdu).
 */
class UserGuideActivity : AppCompatActivity() {

    private data class Chapter(
        @DrawableRes val icon: Int,
        @StringRes val chip: Int,
        @StringRes val title: Int,
        @StringRes val summary: Int,
        @ArrayRes val steps: Int,
        @StringRes val tip: Int?,
        val shots: List<Int>
    )

    private class ChapterViews(val card: View, val body: View, val chevron: ImageView)

    private lateinit var scrollView: NestedScrollView
    private lateinit var chaptersContainer: LinearLayout
    private val chapterViews = mutableListOf<ChapterViews>()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_guide)

        val toolbar = findViewById<Toolbar>(R.id.toolbarUserGuide)
        setSupportActionBar(toolbar)
        // Title is the centred TextView inside the Toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        scrollView = findViewById(R.id.svUserGuide)
        chaptersContainer = findViewById(R.id.llUgChapters)

        val chapters = buildChapters()
        findViewById<TextView>(R.id.tvUgTopicCount).text = getString(R.string.ug_hero_count, chapters.size)

        setupQuickSteps()
        setupChapters(chapters)
        setupHelpButtons()

        // Open the first topic so people see what a topic contains
        if (chapterViews.isNotEmpty()) setExpanded(chapterViews[0], true)
    }

    private fun buildChapters(): List<Chapter> {
        val isUrdu = LocaleHelper.getLanguage(this) == "ur"
        val homeShot = if (isUrdu) R.drawable.ug_shot_home_urdu else R.drawable.ug_shot_home
        return listOf(
            Chapter(R.drawable.ic_status_check, R.string.ug_c1_chip, R.string.ug_c1_title, R.string.ug_c1_summary,
                R.array.ug_c1_steps, R.string.ug_c1_tip, emptyList()),
            Chapter(R.drawable.ic_user, R.string.ug_c2_chip, R.string.ug_c2_title, R.string.ug_c2_summary,
                R.array.ug_c2_steps, R.string.ug_c2_tip, emptyList()),
            Chapter(R.drawable.ic_key, R.string.ug_c3_chip, R.string.ug_c3_title, R.string.ug_c3_summary,
                R.array.ug_c3_steps, R.string.ug_c3_tip, emptyList()),
            Chapter(R.drawable.ic_baseline_home_24, R.string.ug_c4_chip, R.string.ug_c4_title, R.string.ug_c4_summary,
                R.array.ug_c4_steps, R.string.ug_c4_tip, listOf(R.drawable.ug_shot_home_top, homeShot)),
            Chapter(R.drawable.ic_file_text, R.string.ug_c5_chip, R.string.ug_c5_title, R.string.ug_c5_summary,
                R.array.ug_c5_steps, R.string.ug_c5_tip, listOf(R.drawable.ug_shot_form_start, R.drawable.ug_shot_form_end)),
            Chapter(R.drawable.ic_cnic, R.string.ug_c6_chip, R.string.ug_c6_title, R.string.ug_c6_summary,
                R.array.ug_c6_steps, R.string.ug_c6_tip, listOf(R.drawable.ug_shot_scanner)),
            Chapter(R.drawable.ic_chat_bot, R.string.ug_c7_chip, R.string.ug_c7_title, R.string.ug_c7_summary,
                R.array.ug_c7_steps, R.string.ug_c7_tip, listOf(R.drawable.ug_shot_form_assistant)),
            Chapter(R.drawable.ic_mic, R.string.ug_c8_chip, R.string.ug_c8_title, R.string.ug_c8_summary,
                R.array.ug_c8_steps, R.string.ug_c8_tip, listOf(R.drawable.ug_shot_assistant)),
            Chapter(R.drawable.ic_money_bag, R.string.ug_c9_chip, R.string.ug_c9_title, R.string.ug_c9_summary,
                R.array.ug_c9_steps, R.string.ug_c9_tip,
                listOf(R.drawable.ug_shot_ledger, R.drawable.ug_shot_ledger_details, R.drawable.ug_shot_ledger_pay)),
            Chapter(R.drawable.ic_bell, R.string.ug_c10_chip, R.string.ug_c10_title, R.string.ug_c10_summary,
                R.array.ug_c10_steps, R.string.ug_c10_tip, emptyList()),
            Chapter(R.drawable.ic_edit, R.string.ug_c11_chip, R.string.ug_c11_title, R.string.ug_c11_summary,
                R.array.ug_c11_steps, null, listOf(R.drawable.ug_shot_profile)),
            Chapter(R.drawable.ic_baseline_widgets_24, R.string.ug_c12_chip, R.string.ug_c12_title, R.string.ug_c12_summary,
                R.array.ug_c12_steps, null, listOf(R.drawable.ug_shot_settings, R.drawable.ug_shot_settings_more)),
            Chapter(R.drawable.ic_info, R.string.ug_c13_chip, R.string.ug_c13_title, R.string.ug_c13_summary,
                R.array.ug_c13_steps, R.string.ug_c13_tip, listOf(R.drawable.ug_shot_feedback)),
            Chapter(R.drawable.ic_shield_check, R.string.ug_c14_chip, R.string.ug_c14_title, R.string.ug_c14_summary,
                R.array.ug_c14_steps, R.string.ug_c14_tip, emptyList())
        )
    }

    // ---------------- Quick start ----------------

    private fun setupQuickSteps() {
        val container = findViewById<LinearLayout>(R.id.llUgQuickSteps)
        val items = listOf(
            R.drawable.ic_user to R.string.ug_quick_1,
            R.drawable.ic_cnic to R.string.ug_quick_2,
            R.drawable.ic_status_check to R.string.ug_quick_3
        )
        val inflater = LayoutInflater.from(this)
        items.forEachIndexed { index, (icon, text) ->
            val view = inflater.inflate(R.layout.item_user_guide_quick_step, container, false)
            view.findViewById<ImageView>(R.id.ivUgQuickIcon).setImageResource(icon)
            view.findViewById<TextView>(R.id.tvUgQuickNumber).text = (index + 1).toString()
            view.findViewById<TextView>(R.id.tvUgQuickText).setText(text)
            container.addView(view)
        }
    }

    // ---------------- Topics ----------------

    private fun setupChapters(chapters: List<Chapter>) {
        val chipGroup = findViewById<ChipGroup>(R.id.cgUgTopics)
        val inflater = LayoutInflater.from(this)

        chapters.forEachIndexed { index, chapter ->
            val card = inflater.inflate(R.layout.item_user_guide_chapter, chaptersContainer, false)
            val title = getString(chapter.title)
            val summary = getString(chapter.summary)

            card.findViewById<ImageView>(R.id.ivUgChapterIcon).setImageResource(chapter.icon)
            card.findViewById<TextView>(R.id.tvUgChapterNumber).text = String.format(java.util.Locale.US, "%02d", index + 1)
            card.findViewById<TextView>(R.id.tvUgChapterTitle).text = title
            card.findViewById<TextView>(R.id.tvUgChapterSummary).text = summary

            bindShots(card.findViewById(R.id.llUgChapterShots), chapter.shots)
            bindSteps(card.findViewById(R.id.llUgChapterSteps), resources.getStringArray(chapter.steps))

            val tipBox = card.findViewById<View>(R.id.llUgChapterTip)
            if (chapter.tip != null) {
                card.findViewById<TextView>(R.id.tvUgChapterTip).setText(chapter.tip)
            } else {
                tipBox.visibility = View.GONE
            }

            val views = ChapterViews(card, card.findViewById(R.id.llUgChapterBody), card.findViewById(R.id.ivUgChapterChevron))
            val header = card.findViewById<View>(R.id.llUgChapterHeader)
            header.contentDescription = "$title. $summary"
            header.setOnClickListener { setExpanded(views, views.body.visibility != View.VISIBLE) }

            chapterViews.add(views)
            chaptersContainer.addView(card)

            chipGroup.addView(createTopicChip(getString(chapter.chip)) { openChapter(views) })
        }
    }

    private fun createTopicChip(text: String, onClick: () -> Unit): Chip {
        return Chip(this).apply {
            this.text = text
            isCheckable = false
            chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ug_teal_soft))
            setTextColor(ContextCompat.getColor(context, R.color.ug_teal_deep))
            chipStrokeWidth = 0f
            textSize = 13f
            setOnClickListener { onClick() }
        }
    }

    private fun openChapter(views: ChapterViews) {
        setExpanded(views, true)
        // Wait for the expanded card to be laid out, then bring its top into view
        views.card.post {
            val y = chaptersContainer.top + views.card.top - dp(8)
            scrollView.smoothScrollTo(0, y.coerceAtLeast(0))
        }
    }

    private fun setExpanded(views: ChapterViews, expanded: Boolean) {
        views.body.visibility = if (expanded) View.VISIBLE else View.GONE
        views.chevron.animate().rotation(if (expanded) 180f else 0f).setDuration(200).start()
    }

    private fun bindSteps(container: LinearLayout, steps: Array<String>) {
        val inflater = LayoutInflater.from(this)
        steps.forEachIndexed { index, step ->
            val row = inflater.inflate(R.layout.item_user_guide_step, container, false)
            row.findViewById<TextView>(R.id.tvUgStepNumber).text = (index + 1).toString()
            row.findViewById<TextView>(R.id.tvUgStepText).text = step
            container.addView(row)
        }
    }

    /**
     * Tall phone screenshots are shown two side by side (or one, narrower, in the centre);
     * wide crops use the full width.
     */
    private fun bindShots(container: LinearLayout, shots: List<Int>) {
        if (shots.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        var i = 0
        while (i < shots.size) {
            val shot = shots[i]
            if (isTall(shot)) {
                val next = shots.getOrNull(i + 1)
                if (next != null && isTall(next)) {
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = dp(14) }
                    }
                    row.addView(createShot(shot), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { marginEnd = dp(6) })
                    row.addView(createShot(next), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { marginStart = dp(6) })
                    container.addView(row)
                    i += 2
                    continue
                }
                val width = (resources.displayMetrics.widthPixels * 0.55f).toInt()
                container.addView(createShot(shot), LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply {
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                        bottomMargin = dp(14)
                    })
            } else {
                container.addView(createShot(shot), LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(14) })
            }
            i++
        }
    }

    private fun isTall(@DrawableRes res: Int): Boolean {
        val d = ContextCompat.getDrawable(this, res) ?: return false
        return d.intrinsicWidth > 0 && d.intrinsicHeight > d.intrinsicWidth * 1.15f
    }

    private fun createShot(@DrawableRes res: Int): ShapeableImageView {
        return ShapeableImageView(this).apply {
            setImageResource(res)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.ug_screenshot_desc)
            shapeAppearanceModel = ShapeAppearanceModel.builder().setAllCornerSizes(dp(14).toFloat()).build()
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ug_card_stroke))
            strokeWidth = dp(1).toFloat()
            val pad = dp(1) / 2
            setPadding(pad, pad, pad, pad)
        }
    }

    // ---------------- Help ----------------

    private fun setupHelpButtons() {
        findViewById<View>(R.id.btnUgWhatsapp).setOnClickListener { openWhatsApp() }
        findViewById<View>(R.id.btnUgCall).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Constants.SUPPORT_PHONE.replace("-", ""))))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, Constants.SUPPORT_PHONE, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openWhatsApp() {
        val uri = Uri.parse(
            "https://api.whatsapp.com/send?phone=" + Constants.SUPPORT_PHONE_INTL +
                "&text=" + Uri.encode(getString(R.string.whatsapp_support_msg))
        )
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(pkg))
                return
            } catch (e: ActivityNotFoundException) {
                // try the next WhatsApp app
            }
        }
        Toast.makeText(this, R.string.ug_help_whatsapp_missing, Toast.LENGTH_LONG).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
