package com.assan.finance.ui.components

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.assan.finance.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomDateRangePickerFragment : BottomSheetDialogFragment() {

    interface OnDateRangeSelectedListener {
        fun onDateRangeSelected(startDate: Long, endDate: Long)
    }

    private var listener: OnDateRangeSelectedListener? = null
    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null

    private var currentDisplayCalendar: Calendar = Calendar.getInstance()

    private lateinit var tvCurrentMonth: TextView
    private lateinit var rvCalendar: RecyclerView
    private lateinit var rvYears: RecyclerView
    private lateinit var viewFlipper: ViewFlipper

    private lateinit var calendarAdapter: CalendarDateAdapter

    fun setListener(listener: OnDateRangeSelectedListener) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_custom_date_range, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewFlipper = view.findViewById(R.id.viewFlipper)
        rvYears = view.findViewById(R.id.rvYears)
        rvCalendar = view.findViewById(R.id.rvCalendar)
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth)

        val btnCloseYear = view.findViewById<ImageView>(R.id.btnCloseYear)
        val btnCloseCalendar = view.findViewById<ImageView>(R.id.btnCloseCalendar)
        val btnBackToYear = view.findViewById<ImageView>(R.id.btnBackToYear)
        val rlYearHeader = view.findViewById<RelativeLayout>(R.id.rlYearHeader)
        val rlCalendarHeader = view.findViewById<RelativeLayout>(R.id.rlCalendarHeader)

        val btnPrevMonth = view.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNextMonth = view.findViewById<ImageView>(R.id.btnNextMonth)

        val btnClearDates = view.findViewById<TextView>(R.id.btnClearDates)
        val btnSave = view.findViewById<TextView>(R.id.btnSave)

        // Setup Year RecyclerView
        val years = (2020..2035).toList()
        rvYears.layoutManager = GridLayoutManager(context, 3)
        rvYears.adapter = YearAdapter(years) { year ->
            currentDisplayCalendar.set(Calendar.YEAR, year)
            currentDisplayCalendar.set(Calendar.MONTH, Calendar.JANUARY)
            currentDisplayCalendar.set(Calendar.DAY_OF_MONTH, 1)
            
            viewFlipper.displayedChild = 1
            rlYearHeader.visibility = View.GONE
            rlCalendarHeader.visibility = View.VISIBLE
            
            updateCalendarView()
        }

        // Close Buttons
        btnCloseYear.setOnClickListener { dismiss() }
        btnCloseCalendar.setOnClickListener { dismiss() }

        // Back to Year
        btnBackToYear.setOnClickListener {
            viewFlipper.displayedChild = 0
            rlYearHeader.visibility = View.VISIBLE
            rlCalendarHeader.visibility = View.GONE
        }

        // Month Navigation
        btnPrevMonth.setOnClickListener {
            currentDisplayCalendar.add(Calendar.MONTH, -1)
            updateCalendarView()
        }
        btnNextMonth.setOnClickListener {
            currentDisplayCalendar.add(Calendar.MONTH, 1)
            updateCalendarView()
        }

        btnClearDates.setOnClickListener {
            selectedStartDate = null
            selectedEndDate = null
            updateCalendarView()
        }

        btnSave.setOnClickListener {
            if (selectedStartDate != null && selectedEndDate != null) {
                listener?.onDateRangeSelected(selectedStartDate!!.timeInMillis, selectedEndDate!!.timeInMillis)
                dismiss()
            }
        }

        // Initialize Calendar
        rvCalendar.layoutManager = GridLayoutManager(context, 7)
        calendarAdapter = CalendarDateAdapter()
        rvCalendar.adapter = calendarAdapter
    }

    private fun updateCalendarView() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        tvCurrentMonth.text = sdf.format(currentDisplayCalendar.time)
        calendarAdapter.updateMonth(currentDisplayCalendar)
    }

    // --- Adapters ---

    inner class YearAdapter(private val years: List<Int>, private val onYearClick: (Int) -> Unit) : RecyclerView.Adapter<YearAdapter.YearViewHolder>() {
        inner class YearViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvYear: TextView = view.findViewById(R.id.tvYear)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_year, parent, false)
            return YearViewHolder(view)
        }

        override fun onBindViewHolder(holder: YearViewHolder, position: Int) {
            val year = years[position]
            holder.tvYear.text = year.toString()
            holder.itemView.setOnClickListener { onYearClick(year) }
        }

        override fun getItemCount(): Int = years.size
    }

    inner class CalendarDateAdapter : RecyclerView.Adapter<CalendarDateAdapter.DateViewHolder>() {
        
        private val days = ArrayList<Calendar?>()

        fun updateMonth(monthCalendar: Calendar) {
            days.clear()
            val cal = monthCalendar.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, 1)
            
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
            
            // Add empty days for offset
            for (i in 0 until firstDayOfWeek) {
                days.add(null)
            }
            
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in 1..maxDays) {
                val dayCal = cal.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, i)
                days.add(dayCal)
            }
            
            notifyDataSetChanged()
        }

        inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val llDateContainer: LinearLayout = view.findViewById(R.id.llDateContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_date, parent, false)
            return DateViewHolder(view)
        }

        override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
            val date = days[position]
            if (date == null) {
                holder.tvDate.text = ""
                holder.llDateContainer.setBackgroundResource(0)
                holder.itemView.setOnClickListener(null)
                return
            }

            holder.tvDate.text = date.get(Calendar.DAY_OF_MONTH).toString()
            holder.tvDate.setTextColor(Color.parseColor("#1F2937")) // Default Dark Gray
            holder.llDateContainer.setBackgroundResource(0)

            // Selection Logic styling
            if (selectedStartDate != null && isSameDay(date, selectedStartDate!!)) {
                holder.tvDate.setTextColor(Color.WHITE)
                if (selectedEndDate != null && !isSameDay(selectedStartDate!!, selectedEndDate!!)) {
                    holder.llDateContainer.setBackgroundResource(R.drawable.bg_calendar_start)
                } else {
                    holder.llDateContainer.setBackgroundResource(R.drawable.bg_calendar_single)
                }
            } else if (selectedEndDate != null && isSameDay(date, selectedEndDate!!)) {
                holder.tvDate.setTextColor(Color.WHITE)
                holder.llDateContainer.setBackgroundResource(R.drawable.bg_calendar_end)
            } else if (selectedStartDate != null && selectedEndDate != null && date.after(selectedStartDate) && date.before(selectedEndDate)) {
                holder.tvDate.setTextColor(Color.parseColor("#1F2937"))
                holder.llDateContainer.setBackgroundResource(R.drawable.bg_calendar_middle)
            }

            holder.itemView.setOnClickListener {
                if (selectedStartDate == null) {
                    selectedStartDate = date
                } else if (selectedEndDate == null) {
                    if (date.before(selectedStartDate)) {
                        selectedStartDate = date
                    } else {
                        selectedEndDate = date
                    }
                } else {
                    // Reset
                    selectedStartDate = date
                    selectedEndDate = null
                }
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = days.size
        
        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
    }
}
