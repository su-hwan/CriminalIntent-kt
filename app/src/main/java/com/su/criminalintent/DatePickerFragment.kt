package com.su.criminalintent

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import java.util.*

private const val TAG = "DatePickerFragment"
private const val ARG_DATE = "argDate"

class DatePickerFragment() : DialogFragment() {

    companion object {
        fun newInstance(date: Date): DatePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }

            return DatePickerFragment().apply {
                Log.d(TAG, args.toString())
                arguments = args
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val listener = OnDateSetListener { _: DatePicker, year: Int, month: Int, day: Int ->
            val resultDate: Date = GregorianCalendar(year, month, day).time
            targetFragment?.let { fragment ->
                (fragment as Callbacks).onDateSelected(resultDate)
            }
        }

        var cal: Calendar = Calendar.getInstance()
        val date = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_DATE, Date::class.java) as Date
        } else {
            arguments?.getSerializable(ARG_DATE) as Date
        }
        cal.time = date
        val intYear = cal.get(Calendar.YEAR)
        val intMonth = cal.get(Calendar.MONTH)
        val intDay = cal.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(
            requireContext(),
            listener,
            intYear,
            intMonth,
            intDay
        )
    }

    interface Callbacks {
        fun onDateSelected(date: Date)
    }

    private var callbacks: Callbacks? = null
}