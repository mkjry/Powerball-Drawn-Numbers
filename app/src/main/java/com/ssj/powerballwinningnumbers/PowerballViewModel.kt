package com.ssj.powerballwinningnumbers

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PowerballViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PowerballRepository(application)

    private val _numbers = MutableLiveData<PowerballNumbers?>()
    val numbers: LiveData<PowerballNumbers?> = _numbers

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Cache to store the last successfully fetched complete data.
    private var lastSuccessfulNumbers: PowerballNumbers? = null

    fun fetchLatestNumbers() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                // 1. Fetch new data from the repository.
                val newNumbers = repository.getLatestNumbers()

                if (newNumbers != null) {
                    // --- THIS IS THE KEY LOGIC ---
                    // 2. Check if the new data is incomplete (i.e., jackpot amount is "Counting..").
                    if (newNumbers.jackpotAmount == "Counting..") {
                        Log.w("ViewModel", "New data is incomplete. Checking cache.")
                        // 3. If it's incomplete, check if we have cached data for the same date.
                        if (lastSuccessfulNumbers?.drawDate == newNumbers.drawDate) {
                            Log.i("ViewModel", "Cache hit for the same date. Merging cached jackpot data.")
                            // 4. If so, merge the cached (complete) jackpot data into the new data.
                            val mergedNumbers = newNumbers.copy(
                                jackpotAmount = lastSuccessfulNumbers!!.jackpotAmount,
                                cashValue = lastSuccessfulNumbers!!.cashValue
                            )
                            _numbers.value = mergedNumbers
                            // Update the cache with the potentially newer (but now complete) data.
                            lastSuccessfulNumbers = mergedNumbers
                        } else {
                            // No valid cache for this date, post the incomplete data as is.
                            _numbers.value = newNumbers
                        }
                    } else {
                        // The new data is complete.
                        Log.i("ViewModel", "New data is complete. Updating UI and cache.")
                        _numbers.value = newNumbers
                        // 5. Cache the new, complete data.
                        lastSuccessfulNumbers = newNumbers
                    }
                } else {
                    _error.value = "Unable to fetch winning numbers."
                }

            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
}
