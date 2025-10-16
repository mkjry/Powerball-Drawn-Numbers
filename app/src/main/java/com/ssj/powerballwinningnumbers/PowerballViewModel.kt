package com.ssj.powerballwinningnumbers

import android.app.Application
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

    fun checkCacheAndFetch() {
        viewModelScope.launch {
            _loading.postValue(true)
            val cachedNumbers = repository.getLatestNumbers(forceNetwork = false)
            if (cachedNumbers != null && cachedNumbers.jackpotAmount != "Counting..") {
                _numbers.postValue(cachedNumbers)
                _loading.postValue(false)
            } else {
                fetchLatestNumbers()
            }
        }
    }

    fun fetchLatestNumbers(forceNetwork: Boolean = true) {
        viewModelScope.launch {
            _loading.postValue(true)
            try {
                val latestNumbers = repository.getLatestNumbers(forceNetwork = forceNetwork)
                if (latestNumbers != null) {
                    _numbers.postValue(latestNumbers)
                    _error.postValue(null)
                } else {
                    _error.postValue("Unable to fetch winning numbers.")
                }
            } catch (e: Exception) {
                _error.postValue("An unexpected error occurred: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
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