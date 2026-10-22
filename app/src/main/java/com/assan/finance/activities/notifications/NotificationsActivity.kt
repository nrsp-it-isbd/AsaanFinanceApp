package com.assan.finance.activities.notifications

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.assan.finance.R
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import android.graphics.Color
import android.util.Log
import com.assan.finance.sharedPreferences.SharedManager

class NotificationsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.assan.finance.utils.LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Setup Back Button
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val rvNotifications = findViewById<RecyclerView>(R.id.rvNotifications)
        val shimmer = findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerNotifications)
        val llEmptyState = findViewById<LinearLayout>(R.id.llEmptyState)
        
        val hsvFilters = findViewById<android.widget.HorizontalScrollView>(R.id.hsvFilters)
        val tvToday = findViewById<TextView>(R.id.tvToday)
        val rlTodayHeader = findViewById<View>(R.id.rlTodayHeader)
        val swipeRefreshLayout = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        val svEmptyState = findViewById<View>(R.id.svEmptyState)
        // The refresh layout's child is a FrameLayout, so tell it when the visible list is scrolled down
        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            if (rvNotifications.visibility == View.VISIBLE) rvNotifications.canScrollVertically(-1)
            else svEmptyState.canScrollVertically(-1)
        }
        val chipDateRange = findViewById<TextView>(R.id.chipDateRange)

        val chipAll = findViewById<TextView>(R.id.chipAll)
        val chipUnread = findViewById<TextView>(R.id.chipUnread)
        val chipRead = findViewById<TextView>(R.id.chipRead)
        val chipGeneral = findViewById<TextView>(R.id.chipGeneral)
        val chipRecovery = findViewById<TextView>(R.id.chipRecovery)
        val chipInstallment = findViewById<TextView>(R.id.chipInstallment)
        val chipLoan = findViewById<TextView>(R.id.chipLoan)
        val chipPayment = findViewById<TextView>(R.id.chipPayment)
        val chipApproved = findViewById<TextView>(R.id.chipApproved)
        val chipRejected = findViewById<TextView>(R.id.chipRejected)
        val chipFeedback = findViewById<TextView>(R.id.chipFeedback)
        val chipPromotion = findViewById<TextView>(R.id.chipPromotion)
        val chipCOC = findViewById<TextView>(R.id.chipCOC)
        val chipBT = findViewById<TextView>(R.id.chipBT)
        val chipClosedLoan = findViewById<TextView>(R.id.chipClosedLoan)
        val tvMarkRead = findViewById<TextView>(R.id.tvMarkRead)
        
        val typeToView = mapOf(
            "All" to chipAll, "Unread" to chipUnread, "Read" to chipRead,
            "General" to chipGeneral, "Recovery" to chipRecovery, "Installment" to chipInstallment,
            "Financing" to chipLoan, "Payment" to chipPayment, "Approved" to chipApproved,
            "Rejected" to chipRejected, "Feedback" to chipFeedback, "Promotion" to chipPromotion,
            "COC" to chipCOC, "BT" to chipBT, "Closed Loan" to chipClosedLoan
        )

        val sharedManager = SharedManager.getInstance(this)
        val borrowerId = sharedManager.getString("BORROWER_NO", "") ?: ""
        sharedManager.saveBoolean("HAS_UNREAD_NOTIFICATION", false)
        
        val fullList = ArrayList<NotificationModel>()
        val list = ArrayList<NotificationModel>()
        var currentFilter = "All"
        
        val endCal = java.util.Calendar.getInstance()
        endCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        endCal.set(java.util.Calendar.MINUTE, 0)
        endCal.set(java.util.Calendar.SECOND, 0)
        endCal.set(java.util.Calendar.MILLISECOND, 0)
        
        val startCal = java.util.Calendar.getInstance()
        startCal.add(java.util.Calendar.MONTH, -1)
        startCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        startCal.set(java.util.Calendar.MINUTE, 0)
        startCal.set(java.util.Calendar.SECOND, 0)
        startCal.set(java.util.Calendar.MILLISECOND, 0)
        
        var startDateMs: Long? = startCal.timeInMillis
        var endDateMs: Long? = endCal.timeInMillis
        val dateFormat = java.text.SimpleDateFormat("dd-MMM-yyyy HH:mm", java.util.Locale.ENGLISH)
        val displayDateFormat = java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.ENGLISH)
        
        chipDateRange.text = "${displayDateFormat.format(startCal.time)} - ${displayDateFormat.format(endCal.time)}"
        chipDateRange.setBackgroundResource(R.drawable.bg_chip_selected)
        chipDateRange.setTextColor(Color.parseColor("#10B981"))
        chipDateRange.compoundDrawablesRelative[0]?.setTint(Color.parseColor("#10B981"))

        fun updateEmptyState() {
            // No notifications at all: show only the empty message (the date filter alone looked broken)
            if (fullList.isEmpty()) {
                hsvFilters.visibility = View.GONE
                rlTodayHeader.visibility = View.GONE
                tvMarkRead.visibility = View.GONE
            } else {
                hsvFilters.visibility = View.VISIBLE
                rlTodayHeader.visibility = View.VISIBLE
                tvToday.visibility = View.VISIBLE
                tvMarkRead.visibility = View.VISIBLE
            }

            if (list.isEmpty()) {
                rvNotifications.visibility = View.GONE
                llEmptyState.visibility = View.VISIBLE
                svEmptyState.visibility = View.VISIBLE
            } else {
                rvNotifications.visibility = View.VISIBLE
                llEmptyState.visibility = View.GONE
                svEmptyState.visibility = View.GONE
            }
        }

        fun filterList(type: String) {
            currentFilter = type
            // Update Chip UI
            val selectedColor = Color.parseColor("#10B981")
            val unselectedColor = Color.parseColor("#6B7280")
            
            typeToView.values.forEach { chip ->
                chip.setBackgroundResource(R.drawable.bg_pill_grey)
                chip.setTextColor(unselectedColor)
                chip.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
            
            typeToView[type]?.let {
                it.setBackgroundResource(R.drawable.bg_chip_selected)
                it.setTextColor(selectedColor)
                it.setTypeface(null, android.graphics.Typeface.BOLD)
            }

            var filteredList = when (type) {
                "All" -> fullList.toList()
                "Unread" -> fullList.filter { it.isUnread }
                "Read" -> fullList.filter { !it.isUnread }
                else -> fullList.filter { it.category.equals(type, ignoreCase = true) }
            }

            if (startDateMs != null && endDateMs != null) {
                filteredList = filteredList.filter { item ->
                    try {
                        val date = dateFormat.parse(item.date)
                        if (date != null) {
                            val time = date.time
                            time >= startDateMs!! && time <= (endDateMs!! + 86400000)
                        } else true
                    } catch (e: Exception) {
                        true
                    }
                }
            }

            list.clear()
            list.addAll(filteredList)
            rvNotifications.adapter?.notifyDataSetChanged()
            updateEmptyState()
        }

        val adapter = NotificationAdapter(list) { item, position ->
            // On Click Mark as Read
            if (item.isUnread) {
                com.assan.finance.network.assan.AssanRetrofitClient.apiService.markNotificationRead(item.logId).enqueue(object : retrofit2.Callback<com.assan.finance.model.assan.ResponseModelLogin> {
                    override fun onResponse(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelLogin>, response: retrofit2.Response<com.assan.finance.model.assan.ResponseModelLogin>) {
                        if (response.isSuccessful) {
                            item.isUnread = false
                            fullList.find { it.logId == item.logId }?.isUnread = false
                            filterList(currentFilter)
                        }
                    }
                    override fun onFailure(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelLogin>, t: Throwable) {
                        Log.e("NotificationsActivity", "Failed to mark as read: ${t.message}")
                    }
                })
            }
        }
        
        rvNotifications.adapter = adapter
        
        typeToView.forEach { (type, view) ->
            view.setOnClickListener { filterList(type) }
        }

        chipDateRange.setOnClickListener {
            val picker = com.assan.finance.ui.components.CustomDateRangePickerFragment()
            picker.setListener(object : com.assan.finance.ui.components.CustomDateRangePickerFragment.OnDateRangeSelectedListener {
                override fun onDateRangeSelected(startDate: Long, endDate: Long) {
                    startDateMs = startDate
                    endDateMs = endDate
                    
                    if (startDate == endDate) {
                        chipDateRange.text = displayDateFormat.format(java.util.Date(startDate))
                    } else {
                        chipDateRange.text = "${displayDateFormat.format(java.util.Date(startDate))} - ${displayDateFormat.format(java.util.Date(endDate))}"
                    }
                    
                    chipDateRange.setBackgroundResource(R.drawable.bg_chip_selected)
                    chipDateRange.setTextColor(Color.parseColor("#10B981"))
                    chipDateRange.compoundDrawablesRelative[0]?.setTint(Color.parseColor("#10B981"))
                    filterList(currentFilter)
                }
            })
            picker.show(supportFragmentManager, picker.toString())
        }

        tvMarkRead.setOnClickListener {
            val unreadItems = fullList.filter { it.isUnread }
            for (item in unreadItems) {
                item.isUnread = false
                com.assan.finance.network.assan.AssanRetrofitClient.apiService.markNotificationRead(item.logId).enqueue(object : retrofit2.Callback<com.assan.finance.model.assan.ResponseModelLogin> {
                    override fun onResponse(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelLogin>, response: retrofit2.Response<com.assan.finance.model.assan.ResponseModelLogin>) {}
                    override fun onFailure(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelLogin>, t: Throwable) {}
                })
            }
            filterList(currentFilter)
        }

        fun loadNotifications() {
            swipeRefreshLayout.isRefreshing = true
            if (fullList.isEmpty()) {
                shimmer.visibility = View.VISIBLE
                shimmer.startShimmer()
            }
            
            val cnic = sharedManager.getString("CNIC", "") ?: ""
            if (borrowerId.isNotEmpty() || cnic.isNotEmpty()) {
                com.assan.finance.network.assan.AssanRetrofitClient.apiService.getNotifications(borrowerId, cnic).enqueue(object : retrofit2.Callback<com.assan.finance.model.assan.ResponseModelNotifications> {
                    override fun onResponse(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelNotifications>, response: retrofit2.Response<com.assan.finance.model.assan.ResponseModelNotifications>) {
                        swipeRefreshLayout.isRefreshing = false
                        shimmer.stopShimmer()
                        shimmer.visibility = View.GONE
                        
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            if (body.success && body.data != null && body.data.isNotEmpty()) {
                                fullList.clear()
                                for (item in body.data) {
                                    fullList.add(
                                        NotificationModel(
                                            logId = item.logId,
                                            title = item.title ?: getString(R.string.str_notifications),
                                            message = item.message,
                                            date = item.date,
                                            category = if (item.category.isNullOrEmpty()) "GENERAL" else item.category.uppercase(),
                                            emoji = if (item.category?.equals("RECOVERY", true) == true) "💰" else "✉️",
                                            bgColorHex = if (item.category?.equals("RECOVERY", true) == true) "#E8F5E9" else "#F3E8FF",
                                            isUnread = !item.isRead
                                        )
                                    )
                                }
                                fullList.sortByDescending { it.date }
                                filterList(currentFilter)
                            } else {
                                fullList.clear()
                                list.clear()
                                updateEmptyState()
                            }
                        } else {
                            updateEmptyState()
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelNotifications>, t: Throwable) {
                        swipeRefreshLayout.isRefreshing = false
                        shimmer.stopShimmer()
                        shimmer.visibility = View.GONE
                        updateEmptyState()
                        Log.e("NotificationsActivity", "Failed to fetch notifications: ${t.message}")
                    }
                })
            } else {
                swipeRefreshLayout.isRefreshing = false
                shimmer.stopShimmer()
                shimmer.visibility = View.GONE
                updateEmptyState()
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadNotifications()
        }

        loadNotifications()
    }
}
