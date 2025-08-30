package com.ssj.powerballwinningnumbers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PowerballViewModel : ViewModel() {

    private val repository = PowerballRepository()

    private val _numbers = MutableLiveData<PowerballNumbers?>()
    val numbers: LiveData<PowerballNumbers?> = _numbers

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchLatestNumbers() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = repository.getLatestNumbers()
                _numbers.value = result

                if (result == null) {
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