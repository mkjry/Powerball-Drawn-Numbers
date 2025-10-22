package com.ssj.powerballwinningnumbers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: PowerballViewModel by viewModels()

    // UI Elements
    private lateinit var btnFetch: Button
    private lateinit var btnOpenWebsite: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvJackpot: TextView
    private lateinit var tvJackpotAmount: TextView
    private lateinit var tvCashValue: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvDateDetails: TextView
    private lateinit var layoutNumbers: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var tvMultiplier: TextView
    private lateinit var tvJackpotWinners: TextView
    private lateinit var numberViews: List<TextView>
    private lateinit var tvPowerball: TextView

    // ADDED: UI Elements for the 'Next Drawing' section
    private lateinit var layoutNextDraw: LinearLayout
    private lateinit var tvNextDrawDate: TextView
    private lateinit var tvNextDrawJackpot: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d(TAG, "MainActivity onCreate")
        setContentView(R.layout.activity_main)

        initViews()
        setupObservers()
        setupClickListeners()

        // ADDED: Instead of fetching directly, check for a valid cache first.
        viewModel.checkCacheAndFetch()
    }

    // ADDED: onStop, for saving data to SharedPreferences
    override fun onStop() {
        super.onStop()
        // ADDED: Save the current data to SharedPreferences when the app goes into the background.
        Log.d(TAG, "onStop called. Saving data to SharedPreferences.")
        viewModel.saveDataToPrefs()
    }

    private fun initViews() {
        btnFetch = findViewById(R.id.btn_fetch)
        btnOpenWebsite = findViewById(R.id.btn_open_website)
        progressBar = findViewById(R.id.progress_bar)
        tvJackpot = findViewById(R.id.tv_jackpot)
        tvJackpotAmount = findViewById(R.id.tv_jackpot_amount)
        tvCashValue = findViewById(R.id.tv_cash_value)
        tvDate = findViewById(R.id.tv_date)
        tvDateDetails = findViewById(R.id.tv_date_details)
        layoutNumbers = findViewById(R.id.layout_numbers)
        tvError = findViewById(R.id.tv_error)
        tvMultiplier = findViewById(R.id.tv_multiplier)
        tvJackpotWinners = findViewById(R.id.tv_jackpot_winners)

        // ADDED: Initialize new views for the 'Next Drawing' section
        layoutNextDraw = findViewById(R.id.layout_next_draw)
        tvNextDrawDate = findViewById(R.id.tv_next_draw_date)
        tvNextDrawJackpot = findViewById(R.id.tv_next_draw_jackpot)

        numberViews = listOf(
            findViewById(R.id.tv_num1),
            findViewById(R.id.tv_num2),
            findViewById(R.id.tv_num3),
            findViewById(R.id.tv_num4),
            findViewById(R.id.tv_num5)
        )
        tvPowerball = findViewById(R.id.tv_powerball)
    }

    private fun setupObservers() {
        try {
            viewModel.loading.observe(this) { isLoading ->
                android.util.Log.d(TAG, "Loading state: $isLoading")
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                btnFetch.isEnabled = !isLoading
            }

            viewModel.numbers.observe(this) { numbers ->
                android.util.Log.d(TAG, "Numbers received: $numbers")
                if (numbers != null) {
                    displayNumbers(numbers)
                }
            }

            viewModel.error.observe(this) { errorMsg ->
                android.util.Log.d(TAG, "Error received: $errorMsg")
                tvError.text = errorMsg
                tvError.visibility = if (errorMsg != null) View.VISIBLE else View.GONE
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error setting up observers: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        btnFetch.setOnClickListener {
            android.util.Log.d(TAG, "Fetch button clicked")
            // ADDED: The button always fetches fresh data from the network, ignoring the cache.
            getDrawingNumbers()
        }

        btnOpenWebsite.setOnClickListener {
            val url = "https://www.powerball.com/"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    private fun getDrawingNumbers() {
        hideAllDataViews()
        viewModel.fetchLatestNumbers(forceNetwork = true)
    }

    private fun displayNumbers(numbers: PowerballNumbers) {
        android.util.Log.d(TAG, "Displaying numbers: ${numbers.numbers}, powerball: ${numbers.powerball}")

        try {
            // --- Display Winning Draw Information ---
            if (numbers.jackpotAmount.isNotEmpty() && numbers.jackpotAmount != "Counting..") {
                tvJackpot.visibility = View.VISIBLE
                tvJackpotAmount.text = numbers.jackpotAmount
                tvJackpotAmount.visibility = View.VISIBLE
            }

            // ADDED: This part is commented out to hide the Cash Value.
            // if (numbers.cashValue.isNotEmpty() && numbers.cashValue != "Counting..") {
            //     tvCashValue.text = "Cash Value: ${numbers.cashValue}"
            //     tvCashValue.visibility = View.VISIBLE
            // }

            if (numbers.jackpotWinners.equals("None", ignoreCase = true) || numbers.jackpotWinners.isEmpty()) {
                tvJackpotWinners.text = "No Winner"
            } else {
                tvJackpotWinners.text = "Winners: ${numbers.jackpotWinners}"
            }
            tvJackpotWinners.visibility = View.VISIBLE


            tvDate.text = numbers.drawDateFormatted
            tvDate.visibility = View.VISIBLE

            val dateDetails = buildString {
                numbers.drawDateObject?.let { drawDate ->
                    val currentDate = java.util.Date()
                    val diffInMillis = currentDate.time - drawDate.time
                    val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                    when {
                        diffInDays == 0L -> append("Today")
                        diffInDays == 1L -> append("Yesterday")
                        diffInDays > 1 -> append("$diffInDays days ago")
                        diffInDays < 0 -> append("Upcoming")
                    }
                }
            }
            tvDateDetails.text = dateDetails
            // Only show if there is content
            tvDateDetails.visibility = if (dateDetails.isNotEmpty()) View.VISIBLE else View.GONE

            numbers.numbers.forEachIndexed { index, number ->
                if (index < numberViews.size) {
                    numberViews[index].text = number.toString()
                }
            }
            tvPowerball.text = numbers.powerball.toString()
            layoutNumbers.visibility = View.VISIBLE

            // ADDED: This part is commented out to hide the Power Play multiplier.
            // if (numbers.multiplier > 1) {
            //     tvMultiplier.text = "Power Play: ${numbers.multiplier}X"
            //     tvMultiplier.visibility = View.VISIBLE
            // }

            // --- Display Next Drawing Information ---
            if (numbers.nextDrawDate.isNotEmpty() && numbers.nextDrawDate != "N/A") {
                tvNextDrawDate.text = "Next Drawing: ${numbers.nextDrawDate}"
                tvNextDrawJackpot.text = "Estimated Jackpot: ${numbers.nextDrawJackpot}"
                layoutNextDraw.visibility = View.VISIBLE
            }

            tvError.visibility = View.GONE

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error displaying numbers: ${e.message}", e)
            tvError.text = "Error displaying results: ${e.message}"
            tvError.visibility = View.VISIBLE
        }
    }

    private fun hideAllDataViews() {
        tvJackpot.visibility = View.GONE
        tvJackpotAmount.visibility = View.GONE
        tvCashValue.visibility = View.GONE
        tvJackpotWinners.visibility = View.GONE
        tvDate.visibility = View.GONE
        tvDateDetails.visibility = View.GONE
        layoutNumbers.visibility = View.GONE
        tvMultiplier.visibility = View.GONE
        layoutNextDraw.visibility = View.GONE
        tvError.visibility = View.GONE
    }
}
