package com.ssj.powerballwinningnumbers

import android.os.Bundle
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
    private lateinit var progressBar: ProgressBar
    private lateinit var tvJackpot: TextView
    private lateinit var tvJackpotAmount: TextView
    private lateinit var tvCashValue: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvDateDetails: TextView
    private lateinit var layoutNumbers: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var tvMultiplier: TextView
    private lateinit var numberViews: List<TextView>
    private lateinit var tvPowerball: TextView

    // UI Elements for the 'Next Drawing' section
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
        getDrawingNumbers()
    }

    private fun initViews() {
        btnFetch = findViewById(R.id.btn_fetch)
        progressBar = findViewById(R.id.progress_bar)
        tvJackpot = findViewById(R.id.tv_jackpot)
        tvJackpotAmount = findViewById(R.id.tv_jackpot_amount)
        tvCashValue = findViewById(R.id.tv_cash_value)
        tvDate = findViewById(R.id.tv_date)
        tvDateDetails = findViewById(R.id.tv_date_details)
        layoutNumbers = findViewById(R.id.layout_numbers)
        tvError = findViewById(R.id.tv_error)
        tvMultiplier = findViewById(R.id.tv_multiplier)

        // Initialize new views for the 'Next Drawing' section
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
            getDrawingNumbers()
        }
    }

    private fun getDrawingNumbers() {
        // Hide previously displayed content before fetching new data
        hideAllDataViews()
        viewModel.fetchLatestNumbers()
    }

    private fun displayNumbers(numbers: PowerballNumbers) {
        android.util.Log.d(TAG, "Displaying numbers: ${numbers.numbers}, powerball: ${numbers.powerball}")

        try {
            // --- Display Winning Draw Information ---
            // Jackpot info for the winning draw
            if (numbers.jackpotAmount.isNotEmpty() && numbers.jackpotAmount != "N/A") {
                tvJackpot.visibility = View.VISIBLE
                tvJackpotAmount.text = numbers.jackpotAmount
                tvJackpotAmount.visibility = View.VISIBLE
            }
            if (numbers.cashValue.isNotEmpty() && numbers.cashValue != "N/A") {
                tvCashValue.text = "Cash Value: ${numbers.cashValue}"
                tvCashValue.visibility = View.VISIBLE
            }

            // Draw date
            tvDate.text = numbers.drawDateFormatted
            tvDate.visibility = View.VISIBLE

            // Additional date details (e.g., "2 days ago")
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

            // Winning numbers
            numbers.numbers.forEachIndexed { index, number ->
                if (index < numberViews.size) {
                    numberViews[index].text = number.toString()
                }
            }
            tvPowerball.text = numbers.powerball.toString()
            layoutNumbers.visibility = View.VISIBLE

            // Multiplier
            if (numbers.multiplier > 1) {
                tvMultiplier.text = "Power Play: ${numbers.multiplier}X"
                tvMultiplier.visibility = View.VISIBLE
            }

            // --- Display Next Drawing Information ---
            if (numbers.nextDrawDate.isNotEmpty() && numbers.nextDrawDate != "N/A") {
                tvNextDrawDate.text = "Next Drawing: ${numbers.nextDrawDate}"
                tvNextDrawJackpot.text = "Estimated Jackpot: ${numbers.nextDrawJackpot}"
                layoutNextDraw.visibility = View.VISIBLE
            }

            // Hide any error messages if data is displayed successfully
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
        tvDate.visibility = View.GONE
        tvDateDetails.visibility = View.GONE
        layoutNumbers.visibility = View.GONE
        tvMultiplier.visibility = View.GONE
        layoutNextDraw.visibility = View.GONE // Hide the new layout as well
        tvError.visibility = View.GONE
    }
}
