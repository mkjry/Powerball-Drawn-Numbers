package com.ssj.powerballwinningnumbers

import java.util.Date

data class PowerballNumbers(
    val drawDate: String,           // Original date string from website
    val drawDateFormatted: String,  // User-friendly formatted date
    val drawDateObject: Date?,      // Actual Date object for calculations
    val numbers: List<Int>,         // Regular numbers (5 numbers)
    val powerball: Int,             // Powerball number
    val multiplier: Int = 1         // Power Play multiplier
)