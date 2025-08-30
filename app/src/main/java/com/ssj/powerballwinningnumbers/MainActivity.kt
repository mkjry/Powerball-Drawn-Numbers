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

    private lateinit var btnFetch: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvDate: TextView
    private lateinit var tvDateDetails: TextView
    private lateinit var layoutNumbers: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var tvMultiplier: TextView

    private lateinit var numberViews: List<TextView>
    private lateinit var tvPowerball: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d(TAG, "MainActivity onCreate")
        setContentView(R.layout.activity_main)

        initViews()
        setupObservers()
        setupClickListeners()
    }

    private fun initViews() {
        btnFetch = findViewById(R.id.btn_fetch)
        progressBar = findViewById(R.id.progress_bar)
        tvDate = findViewById(R.id.tv_date)
        tvDateDetails = findViewById(R.id.tv_date_details)
        layoutNumbers = findViewById(R.id.layout_numbers)
        tvError = findViewById(R.id.tv_error)
        tvMultiplier = findViewById(R.id.tv_multiplier)

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
            viewModel.fetchLatestNumbers()
        }
    }

    private fun displayNumbers(numbers: PowerballNumbers) {
        android.util.Log.d(TAG, "Displaying numbers: ${numbers.numbers}, powerball: ${numbers.powerball}")

        // Display main formatted date
        tvDate.text = numbers.drawDateFormatted
        tvDate.visibility = View.VISIBLE

        // Display additional date information
        val dateDetails = buildString {
            append("Drawing: ${numbers.drawDate}")

            // Add days ago calculation if date object is available
            numbers.drawDateObject?.let { drawDate ->
                val currentDate = java.util.Date()
                val diffInMillis = currentDate.time - drawDate.time
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

                when {
                    diffInDays == 0L -> append(" (Today)")
                    diffInDays == 1L -> append(" (Yesterday)")
                    diffInDays > 1 -> append(" ($diffInDays days ago)")
                    diffInDays < 0 -> append(" (Upcoming)")
                }
            }
        }
        tvDateDetails.text = dateDetails
        tvDateDetails.visibility = View.VISIBLE

        // Display regular numbers
        numbers.numbers.forEachIndexed { index, number ->
            if (index < numberViews.size) {
                numberViews[index].text = number.toString()
            }
        }

        // Display powerball number
        tvPowerball.text = numbers.powerball.toString()

        // Display multiplier
        if (numbers.multiplier > 1) {
            tvMultiplier.text = "Power Play: ${numbers.multiplier}X"
            tvMultiplier.visibility = View.VISIBLE
        } else {
            tvMultiplier.visibility = View.GONE
        }

        // Show numbers layout
        layoutNumbers.visibility = View.VISIBLE

        // Hide error message
        tvError.visibility = View.GONE
    }
}