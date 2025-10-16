package com.ssj.powerballwinningnumbers

// Data classes for parsing the __NEXT_DATA__ JSON blob
// All fields are nullable to prevent crashing if the structure changes.

data class NextData(
    val props: Props?
)

data class Props(
    val pageProps: PageProps?
)

data class PageProps(
    val winningNumbersData: WinningNumbersData?
)

data class WinningNumbersData(
    val drawDate: String?,
    val numbers: List<String>?,
    val powerball: String?,
    val multiplier: String?,
    val prizeAmount: String?,
    val cashValue: String?,
    val nextDrawJackpot: String?,
    val nextDrawDate: String?,
    val jackpotWinners: String?
)
