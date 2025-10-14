package com.ssj.powerballwinningnumbers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

class PowerballRepository(private val context: Context) {

    companion object {
        private const val TAG = "PowerballRepo"
        private val BASE_URL = "https://www.powerball.com/"
        private val DATE_PATTERNS = listOf(
            "EEE, MMM dd, yyyy", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "EEEE, MMMM dd, yyyy", "MMM dd, yyyy", "MMMM dd, yyyy",
            "yyyy-MM-dd", "MM/dd/yyyy"
        )
    }

    suspend fun getLatestNumbers(): PowerballNumbers? {
        return withContext(Dispatchers.IO) {
            try {
                // --- Step 1: Main Page Processing ---
                Log.d(TAG, "[Step 1] Fetching main page: $BASE_URL")
                val mainPageHtml = withContext(Dispatchers.Main) {
                    getHtmlWithWebView(BASE_URL)
                }
                if (mainPageHtml == null) {
                    Log.e(TAG, "[Step 1] Failed to get HTML from main page. Aborting.")
                    return@withContext null
                }

                // --- Step 1.2: Parse Main Page ---
                val mainDoc = Jsoup.parse(mainPageHtml)
                val (initialNumbers, detailPageUrl) = parseFromHtml(mainDoc)

                var finalNumbers = initialNumbers

                if (finalNumbers == null) {
                    Log.e(TAG, "[Step 1] Failed to parse initial numbers from main page. Aborting.")
                    return@withContext null
                }

                if (detailPageUrl != null) {
                    // --- Step 2: Detail Page Processing ---
                    Log.d(TAG, "[Step 2] Fetching detail page: $detailPageUrl")
                    val detailPageHtml = withContext(Dispatchers.Main) {
                        getHtmlWithWebView(detailPageUrl, isDetailPage = true)
                    }

                    if (detailPageHtml != null) {
                        // --- Step 2.2: Parse Detail Page ---
                        val detailDoc = Jsoup.parse(detailPageHtml)
                        val (jackpot, cashValue) = fetchAndParseJackpotDetails(detailDoc)

                        // --- Final Combination ---
                        Log.i(TAG, "All data successfully parsed. Updating final object.")
                        finalNumbers = finalNumbers.copy(
                            jackpotAmount = jackpot,
                            cashValue = cashValue
                        )
                    } else {
                        Log.w(TAG, "[Step 2] Failed to get HTML from detail page. Using partial data.")
                    }
                } else {
                    Log.w(TAG, "[Step 1] 'View Results' button not found. Using partial data.")
                }

                return@withContext finalNumbers

            } catch (e: Exception) {
                Log.e(TAG, "An exception occurred in getLatestNumbers: ${e.message}", e)
                return@withContext null
            }
        }
    }

    private suspend fun getHtmlWithWebView(url: String, isDetailPage: Boolean = false): String? = suspendCancellableCoroutine { continuation ->
        val pageLoadHandler = Handler(Looper.getMainLooper())
        var isFinished = false

        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.blockNetworkImage = true

        val extractHtmlRunnable = Runnable {
            if (!isFinished && continuation.isActive) {
                isFinished = true
                Log.d(TAG, "Debounce timer fired. Evaluating JavaScript for $url")
                webView.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { htmlResult ->
                    val unescapedHtml = htmlResult?.removeSurrounding("\"")?.replace("\\u003C", "<")?.replace("\\n", "\n")?.replace("\\t", "\t")?.replace("\\\"", "\"")
                    if (continuation.isActive) {
                        continuation.resume(unescapedHtml)
                    }
                    destroyWebView(webView)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished called for URL: $url")
                pageLoadHandler.removeCallbacks(extractHtmlRunnable)
                pageLoadHandler.postDelayed(extractHtmlRunnable, 500)
            }
        }

        val timeout = if (isDetailPage) 15000L else 25000L
        val timeoutHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!isFinished && continuation.isActive) {
                isFinished = true
                Log.e(TAG, "WebView loading timed out for url: $url")
                pageLoadHandler.removeCallbacks(extractHtmlRunnable)
                continuation.resume(null)
                destroyWebView(webView)
            }
        }

        continuation.invokeOnCancellation {
            timeoutHandler.removeCallbacks(timeoutRunnable)
            pageLoadHandler.removeCallbacks(extractHtmlRunnable)
            destroyWebView(webView)
        }

        timeoutHandler.postDelayed(timeoutRunnable, timeout)
        webView.loadUrl(url)
    }

    private fun destroyWebView(webView: WebView?) {
        Handler(Looper.getMainLooper()).post {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    private fun parseFromHtml(doc: Document): Pair<PowerballNumbers?, String?> {
        Log.d(TAG, "Starting parseFromHtml for main page...")
        val winningNumbersCard = doc.select("div#numbers div.card").first()
        if (winningNumbersCard == null) {
            Log.e(TAG, "Could not find winning numbers card ('div#numbers div.card').")
            return Pair(null, null)
        }

        val numbers = findWinningNumbers(winningNumbersCard)
        val powerball = findPowerballNumber(winningNumbersCard)
        val drawDateStr = findDrawDate(winningNumbersCard)

        if (numbers.size != 5 || powerball == 0 || drawDateStr == "Unknown Date") {
            Log.e(TAG, "Failed to parse essential data. Numbers: ${numbers.size}, Powerball: $powerball, Date: $drawDateStr")
            return Pair(null, null)
        }
        Log.i(TAG, "Parsed essential data: Nums=${numbers.joinToString()}, PB=$powerball, Date='$drawDateStr'")

        val (formattedDate, dateObject, urlDate) = parseAndFormatDate(drawDateStr)
        val multiplier = findMultiplier(winningNumbersCard)
        val (nextDate, nextAmount) = findNextDrawInfo(doc)

        val viewResultsButton = winningNumbersCard.select("a[href*=draw-result]").first()
        val detailPageUrl = if (viewResultsButton != null) {
            val relativeUrl = viewResultsButton.attr("href")
            Log.d(TAG, "Found 'View Results' button with URL: $relativeUrl")
            if (relativeUrl.startsWith("http")) relativeUrl else "$BASE_URL${relativeUrl.removePrefix("/")}"
        } else {
            Log.w(TAG, "Could not find 'View Results' button. Will not fetch jackpot details.")
            null
        }

        val initialNumbers = PowerballNumbers(
            drawDate = drawDateStr,
            drawDateFormatted = formattedDate,
            drawDateObject = dateObject,
            numbers = numbers,
            powerball = powerball,
            multiplier = multiplier,
            jackpotAmount = "Counting..",
            cashValue = "Counting..", // Also set a placeholder here
            nextDrawDate = nextDate,
            nextDrawJackpot = nextAmount
        )
        return Pair(initialNumbers, detailPageUrl)
    }

    private fun fetchAndParseJackpotDetails(detailDoc: Document): Pair<String, String> {
        Log.d(TAG, "[Step 2] Starting fetchAndParseJackpotDetails...")
        val jackpotSelector = "div.estimated-jackpot span:last-child"
        val cashValueSelector = "div.cash-value span:last-child"
        val jackpot = detailDoc.select(jackpotSelector).first()?.text()?.trim()
        val cashValue = detailDoc.select(cashValueSelector).first()?.text()?.trim()

        if (jackpot.isNullOrEmpty()) {
            Log.w(TAG, "[Step 2] Failed to parse Jackpot using selector: '$jackpotSelector'")
        }
        if (cashValue.isNullOrEmpty()) {
            Log.w(TAG, "[Step 2] Failed to parse Cash Value using selector: '$cashValueSelector'")
        }

        val finalJackpot = if (jackpot.isNullOrEmpty()) "Counting.." else jackpot
        val finalCashValue = if (cashValue.isNullOrEmpty()) "Counting.." else cashValue

        Log.i(TAG, "[Step 2] Parsed from detail page -> Jackpot: '$finalJackpot', Cash Value: '$finalCashValue'")
        return Pair(finalJackpot, finalCashValue)
    }

    private fun findWinningNumbers(card: Element): List<Int> {
        val selector = "div.white-balls"
        val elements = card.select(selector)
        if (elements.isNotEmpty()) {
            val numbers = elements.mapNotNull { it.text().toIntOrNull() }
            if (numbers.size == 5) return numbers
        }
        return emptyList()
    }

    private fun findPowerballNumber(card: Element): Int {
        val selector = "div.powerball"
        return card.select(selector).first()?.text()?.toIntOrNull() ?: 0
    }

    private fun findDrawDate(card: Element): String {
        val selector = "h5.title-date"
        return card.select(selector).first()?.text()?.trim() ?: "Unknown Date"
    }

    private fun findMultiplier(card: Element): Int {
        val selector = "span.multiplier"
        val text = card.select(selector).first()?.text()
        return text?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 1
    }

    private fun findNextDrawInfo(doc: Document): Pair<String, String> {
        val nextDrawingCard = doc.select("div#next-drawing div.card").first()
        if (nextDrawingCard == null) {
            Log.w(TAG, "Could not find 'Next Drawing' card.")
            return Pair("N/A", "N/A")
        }

        val date = nextDrawingCard.select("h5.title-date").first()?.text()?.trim()
        val amount = nextDrawingCard.select("span.game-jackpot-number").first()?.text()?.trim()

        if (date.isNullOrEmpty() || amount.isNullOrEmpty()) {
            Log.w(TAG, "Could not find date or amount in 'Next Drawing' card.")
            return Pair("N/A", "N/A")
        }

        Log.i(TAG, "Constructed Next Draw Info -> Date: '$date', Amount: '$amount'")
        return Pair(date, amount)
    }

    private fun parseAndFormatDate(dateString: String): Triple<String, Date?, String?> {
        for (pattern in DATE_PATTERNS) {
            try {
                val date = SimpleDateFormat(pattern, Locale.US).parse(dateString)
                if (date != null) {
                    val outputFormatter = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)
                    val urlFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    return Triple(outputFormatter.format(date), date, urlFormatter.format(date))
                }
            } catch (e: ParseException) {
                // Continue to the next pattern
            }
        }
        return Triple(dateString, null, null)
    }
}
