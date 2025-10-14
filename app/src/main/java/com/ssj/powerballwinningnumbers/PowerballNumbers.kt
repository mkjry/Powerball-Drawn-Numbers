package com.ssj.powerballwinningnumbers

import java.util.Date

data class PowerballNumbers(
    val drawDate: String,
    val drawDateFormatted: String,
    val drawDateObject: Date?,
    val numbers: List<Int>,
    val powerball: Int,
    val multiplier: Int,
    val jackpotAmount: String,
    val cashValue: String,
    val nextDrawDate: String, // Field for the next drawing date
    val nextDrawJackpot: String // Field for the next drawing's jackpot amount
)