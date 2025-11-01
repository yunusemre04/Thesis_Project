package com.example.positiondeterminer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.positiondeterminer.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class DeepLearningUiState {
    object Idle : DeepLearningUiState()
    object Collecting : DeepLearningUiState()
    object Predicting : DeepLearningUiState()
    data class Success(
        val prediction: String,
        val confidence: Double,
        val deviceMetrics: DeviceMetrics,
        val apiMetrics: ApiMetrics?,
        val allProbabilities: Map<String, Double>
    ) : DeepLearningUiState()
    data class Error(val message: String) : DeepLearningUiState()
}

class DeepLearningViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorCollector = SensorDataCollector(application)
    private val storageService = StorageService(application)
    private val metricsCollector = DeviceMetricsCollector(application)
    
    private val _uiState = MutableStateFlow<DeepLearningUiState>(DeepLearningUiState.Idle)
    val uiState: StateFlow<DeepLearningUiState> = _uiState.asStateFlow()
    
    private val _modelInfo = MutableStateFlow<ModelInfoResponse?>(null)
    val modelInfo: StateFlow<ModelInfoResponse?> = _modelInfo.asStateFlow()
    
    init {
        loadModelInfo()
    }
    
    private fun loadModelInfo() {
        viewModelScope.launch {
            try {
                val response = ApiService.api.dlInfo()
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success) {
                        _modelInfo.value = apiResponse.model_info
                    }
                }
            } catch (e: Exception) {
                // Silently fail, model info is optional
            }
        }
    }
    
    fun startPrediction() {
        viewModelScope.launch {
            try {
                _uiState.value = DeepLearningUiState.Collecting
                
                // Capture initial metrics snapshot
                val startSnapshot = metricsCollector.captureSnapshot()
                
                // Collect sensor data
                sensorCollector.collectSensorData()
                    .take(1) // Take one window of data
                    .collect { readings ->
                        _uiState.value = DeepLearningUiState.Predicting
                        
                        // Process readings to features
                        val features = sensorCollector.processReadingsToFeatures(readings)
                        
                        // Make prediction
                        val request = PredictRequest(sensor_data = features)
                        val response = ApiService.api.dlPredict(request)
                        
                        if (response.isSuccessful && response.body() != null) {
                            val apiResponse = response.body()!!
                            if (apiResponse.success) {
                                val prediction = apiResponse.prediction
                                
                                // Capture final metrics snapshot
                                val endSnapshot = metricsCollector.captureSnapshot()
                                
                                // Calculate device metrics from snapshots
                                val deviceMetrics = metricsCollector.calculateMetrics(startSnapshot, endSnapshot)
                                
                                // API metrics (from server)
                                val apiMetrics = apiResponse.api_metrics
                                
                                // Save to history
                                storageService.saveResult(
                                    PredictionResult(
                                        id = UUID.randomUUID().toString(),
                                        activity = prediction.activity,
                                        confidence = prediction.confidence / 100.0, // API sends percentage
                                        timestamp = System.currentTimeMillis(),
                                        type = "DL",
                                        deviceMetrics = deviceMetrics,
                                        apiMetrics = apiMetrics,
                                        allProbabilities = prediction.all_probabilities
                                    )
                                )
                                
                                _uiState.value = DeepLearningUiState.Success(
                                    prediction = prediction.activity,
                                    confidence = prediction.confidence / 100.0, // API sends percentage
                                    deviceMetrics = deviceMetrics,
                                    apiMetrics = apiMetrics,
                                    allProbabilities = prediction.all_probabilities
                                )
                            } else {
                                _uiState.value = DeepLearningUiState.Error("Prediction failed")
                            }
                        } else {
                            _uiState.value = DeepLearningUiState.Error("Server error: ${response.message()}")
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = DeepLearningUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun reset() {
        _uiState.value = DeepLearningUiState.Idle
    }
}
