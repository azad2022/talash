package com.example.ui.util

import java.util.Calendar
import java.util.Locale

object JalaliCalendar {
    fun getJalaliDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gYear, gMonth, gDay)
    }

    fun getJalaliDateTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val jalaliDate = gregorianToJalali(gYear, gMonth, gDay)
        val timeStr = String.format(Locale.US, "%02d:%02d", hour, minute)
        return "$jalaliDate $timeStr"
    }

    fun getJalaliDateTimeWithSeconds(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        
        val jalaliDate = gregorianToJalali(gYear, gMonth, gDay)
        val timeStr = String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second)
        return "$timeStr - $jalaliDate"
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): String {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gDayNo = gy - 1600
        var i = 1
        while (i < gm) {
            gDayNo += gDaysInMonth[i]
            i++
        }
        if (gm > 2 && ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0)) {
            gDayNo++
        }
        gDayNo += gd - 1 + 365 * (gy - 1600) + (gy - 1597) / 4 - (gy - 1597) / 100 + (gy - 1597) / 400

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        while (jm < 11 && jDayNo >= jDaysInMonth[jm + 1]) {
            jDayNo -= jDaysInMonth[jm + 1]
            jm++
        }
        jm++
        val jd = jDayNo + 1

        return String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    }
}
