package com.ssj.powerballwinningnumbers

import com.google.gson.annotations.Expose
import java.util.Date

data class PowerballNumbers(
    val drawDate: String,
    val drawDateFormatted: String,
    @Expose(serialize = false, deserialize = false)
    val drawDateObject: Date?,
    val numbers: List<Int>,
    val powerball: Int,
    val multiplier: Int,
    val jackpotAmount: String,
    val cashValue: String,
    val jackpotWinners: String,
    val nextDrawDate: String,
    @Expose(serialize = false, deserialize = false)
    val nextDrawDateObject: Date?,
    val nextDrawJackpot: String
)
