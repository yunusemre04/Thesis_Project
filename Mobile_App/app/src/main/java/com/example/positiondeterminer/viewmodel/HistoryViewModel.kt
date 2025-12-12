package com.example.positiondeterminer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.positiondeterminer.data.PredictionResult
import com.example.positiondeterminer.data.StorageService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val storageService = StorageService(application)
    
    val history: StateFlow<List<PredictionResult>> = storageService.getHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    
    fun clearHistoryByType(type: String) {
        viewModelScope.launch {
            storageService.clearHistoryByType(type)
        }
    }
}
