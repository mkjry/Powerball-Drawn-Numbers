package com.ssj.powerballwinningnumbers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// 2. ViewModel()을 AndroidViewModel(application)으로 변경
class PowerballViewModel(application: Application) : AndroidViewModel(application) {

    // 3. 생성자로 받은 application 객체를 Repository에 전달
    private val repository = PowerballRepository(application)

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
