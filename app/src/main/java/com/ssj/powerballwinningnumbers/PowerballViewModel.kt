package com.ssj.powerballwinningnumbers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class PowerballViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PowerballRepository(application)

    private val _numbers = MutableLiveData<PowerballNumbers?>()
    val numbers: LiveData<PowerballNumbers?> = _numbers

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun checkCacheAndFetch() {
        viewModelScope.launch {
            _loading.postValue(true)
            val cachedNumbers = repository.getLatestNumbers(forceNetwork = false)

            if (cachedNumbers == null) {
                // If there's no cache, always fetch.
                fetchLatestNumbers(forceNetwork = true)
                return@launch
            }

            // If cache exists, display it immediately.
            _numbers.postValue(cachedNumbers)

            val drawDateIsPast = isPastDate(cachedNumbers.drawDateObject)
            val nextDrawDateIsTodayOrPast = isTodayOrPast(cachedNumbers.nextDrawDateObject)

            // Fetch new data only if the cached draw is in the past AND the next draw date has passed.
            if (drawDateIsPast && nextDrawDateIsTodayOrPast) {
                fetchLatestNumbers(forceNetwork = true)
            } else {
                // Otherwise, the cached data is the most recent, so no network call is needed.
                _loading.postValue(false)
            }
        }
    }

    fun fetchLatestNumbers(forceNetwork: Boolean = true) {
        viewModelScope.launch {
            // When called directly (e.g., from a button), always show loading and fetch.
            if (forceNetwork) {
                _loading.postValue(true)
            }
            try {
                val newNumbers = repository.getLatestNumbers(forceNetwork = forceNetwork)
                val oldNumbers = _numbers.value

                if (newNumbers != null) {
                    // Merge logic to preserve complete data
                    val finalNumbers = if (oldNumbers != null &&
                        newNumbers.drawDate == oldNumbers.drawDate &&
                        newNumbers.jackpotAmount == "Counting.." &&
                        oldNumbers.jackpotAmount != "Counting.."
                    ) {
                        newNumbers.copy(
                            jackpotAmount = oldNumbers.jackpotAmount,
                            cashValue = oldNumbers.cashValue,
                            jackpotWinners = oldNumbers.jackpotWinners
                        )
                    } else {
                        newNumbers
                    }
                    _numbers.postValue(finalNumbers)
                    _error.postValue(null)
                } else {
                    if (_numbers.value == null) {
                        _error.postValue("Unable to fetch winning numbers.")
                    }
                }
            } catch (e: Exception) {
                if (_numbers.value == null) {
                    _error.postValue("An unexpected error occurred: ${e.message}")
                }
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun isPastDate(date: Date?): Boolean {
        if (date == null) return false
        val today = Calendar.getInstance()
        val drawCal = Calendar.getInstance()
        drawCal.time = date

        // Compare year and day of year for a clean date-only comparison
        return when {
            drawCal.get(Calendar.YEAR) < today.get(Calendar.YEAR) -> true
            drawCal.get(Calendar.YEAR) > today.get(Calendar.YEAR) -> false
            else -> drawCal.get(Calendar.DAY_OF_YEAR) < today.get(Calendar.DAY_OF_YEAR)
        }
    }

    private fun isTodayOrPast(date: Date?): Boolean {
        if (date == null) return true // If next draw date is unknown, refresh to be safe
        val today = Calendar.getInstance()
        val drawCal = Calendar.getInstance()
        drawCal.time = date

        // Compare year and day of year
        return when {
            drawCal.get(Calendar.YEAR) < today.get(Calendar.YEAR) -> true
            drawCal.get(Calendar.YEAR) > today.get(Calendar.YEAR) -> false
            else -> drawCal.get(Calendar.DAY_OF_YEAR) <= today.get(Calendar.DAY_OF_YEAR)
        }
    }

    fun saveDataToPrefs() {
        viewModelScope.launch {
            _numbers.value?.let {
                repository.saveToCache(it)
            }
        }
    }
}