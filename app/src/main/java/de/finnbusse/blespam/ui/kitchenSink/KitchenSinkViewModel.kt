package de.finnbusse.blespam.ui.kitchenSink

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.finnbusse.blespam.Models.LogEntryModel

class KitchenSinkViewModel: ViewModel() {

    private val _statusText = MutableLiveData<String>().apply {
        value = "Start Advertising"
    }
    public fun setStatusText(text:String){
        _statusText.postValue(text)
    }

    val isTransmitting = MutableLiveData<Boolean>().apply {
        value = false
    }

    val logEntries = MutableLiveData<List<LogEntryModel>>().apply {
        value = listOf()
    }

    fun addLogEntry(logEntry: LogEntryModel){
        logEntries.value = logEntries.value?.plus(logEntry) ?: listOf(logEntry)
    }

    val statusText: LiveData<String> = _statusText
}