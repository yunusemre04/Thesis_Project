package com.example.positiondeterminer.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.positiondeterminer.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.sqrt

sealed class FederatedLearningUiState {
    object Idle : FederatedLearningUiState()
    object InitializingModel : FederatedLearningUiState() // NEW: Model download/load
    object Collecting : FederatedLearningUiState()
    object Predicting : FederatedLearningUiState()
    data class Success(
        val prediction: String,
        val confidence: Double,
        val deviceMetrics: DeviceMetrics,
        val apiMetrics: ApiMetrics?,
        val allProbabilities: Map<String, Double>,
        val showFeedback: Boolean = false,
        val isOnDevice: Boolean = true, // NEW: Flag for on-device inference
        val fullFlowMetrics: FullFlowMetrics? = null // NEW: Full FL flow metrics
    ) : FederatedLearningUiState()
    object Training : FederatedLearningUiState()
    data class TrainingSuccess(val message: String) : FederatedLearningUiState()
    data class Error(val message: String) : FederatedLearningUiState()
}

class FederatedLearningViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorCollector = SensorDataCollector(application)
    private val storageService = StorageService(application)
    private val metricsCollector = DeviceMetricsCollector(application)
    private val pytorchFLService = PyTorchFLService(application) // NEW: PyTorch FL service
    
    private val _uiState = MutableStateFlow<FederatedLearningUiState>(FederatedLearningUiState.Idle)
    val uiState: StateFlow<FederatedLearningUiState> = _uiState.asStateFlow()
    
    private val _modelInfo = MutableStateFlow<ModelInfoResponse?>(null)

    
    // Model status state
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()
    
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()
    
    private var lastSensorData: List<Double>? = null
    private var lastPredictionClass: Int? = null
    private var lastGradients: FloatArray? = null // NEW: Store computed gradients
    
    init {
        loadModelInfo()
        initializeModelOnStartup() // Auto-initialize on app startup
    }
    
    /**
     * Initialize model on app startup
     * If bundled in assets, copy to internal storage
     * If not available, show download option
     */
    private fun initializeModelOnStartup() {
        viewModelScope.launch {
            try {
                Log.d("FLViewModel", "Initializing PyTorch model on startup...")
                
                // Try to load model directly (loadModel will auto-copy from assets if needed)
                val loaded = pytorchFLService.loadModel()
                _isModelReady.value = loaded
                
                if (loaded) {
                    Log.d("FLViewModel", "✅ PyTorch model loaded on startup")
                } else {
                    Log.e("FLViewModel", "❌ Failed to load model on startup")
                }
            } catch (e: Exception) {
                Log.e("FLViewModel", "Error during model initialization", e)
                _isModelReady.value = false
            }
        }
    }
    
    private fun loadModelInfo() {
        viewModelScope.launch {
            try {
                val response = ApiService.api.flInfo()
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success) {
                        _modelInfo.value = apiResponse.model_info
                    }
                }
            } catch (e: Exception) {
                Log.e("FLViewModel", "Error loading model info: ${e.message}")
            }
        }
    }
    

    /**
     * Download updated PyTorch model from server (manual update action)
     * This allows users to get the latest model version with federated improvements
     */
    fun downloadPyTorchModel() {
        viewModelScope.launch {
            try {
                _isDownloading.value = true
                _uiState.value = FederatedLearningUiState.InitializingModel
                
                Log.d("FLViewModel", "📥 Downloading updated PyTorch model from server...")
                val downloaded = pytorchFLService.downloadModelFromServer()
                
                if (downloaded) {
                    // Load updated model
                    val loaded = pytorchFLService.loadModel()
                    _isModelReady.value = loaded
                    
                    if (loaded) {
                        _uiState.value = FederatedLearningUiState.Idle
                        Log.d("FLViewModel", "✅ Updated model downloaded and loaded")
                    } else {
                        _uiState.value = FederatedLearningUiState.Error("Downloaded model but failed to load")
                        Log.e("FLViewModel", "❌ Model downloaded but load failed")
                    }
                } else {
                    _isModelReady.value = false
                    _uiState.value = FederatedLearningUiState.Error("Failed to download model. Check network connection.")
                    Log.e("FLViewModel", "❌ Failed to download updated model")
                }
                
            } catch (e: Exception) {
                _isModelReady.value = false
                _uiState.value = FederatedLearningUiState.Error("Download failed: ${e.message}")
                Log.e("FLViewModel", "Model download failed", e)
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun startPrediction() {
        viewModelScope.launch {
            try {
                // Check if model is ready
                if (!_isModelReady.value) {
                    _uiState.value = FederatedLearningUiState.Error("Model not ready. Please download the model first.")
                    return@launch
                }
                
                _uiState.value = FederatedLearningUiState.Collecting
                
                // Capture initial metrics snapshot
                val startSnapshot = metricsCollector.captureSnapshot()
                
                sensorCollector.collectSensorData()
                    .take(1)
                    .collect { readings ->
                        _uiState.value = FederatedLearningUiState.Predicting
                        
                        val features = sensorCollector.processReadingsToFeatures(readings)
                        lastSensorData = features
                        
                        // TRUE FEDERATED LEARNING: On-device inference using PyTorch
                        val predictionResult = pytorchFLService.predict(features)
                        
                        if (predictionResult != null) {
                            lastPredictionClass = predictionResult.classIndex
                            
                            // Capture final metrics snapshot
                            val endSnapshot = metricsCollector.captureSnapshot()
                            
                            // Calculate device metrics from snapshots
                            val deviceMetrics = metricsCollector.calculateMetrics(startSnapshot, endSnapshot)
                            
                            // No API metrics - everything runs on-device!
                            val apiMetrics = null
                            
                            storageService.saveResult(
                                PredictionResult(
                                    id = UUID.randomUUID().toString(),
                                    activity = predictionResult.activity,
                                    confidence = predictionResult.confidence / 100.0,
                                    timestamp = System.currentTimeMillis(),
                                    type = "FL",
                                    deviceMetrics = deviceMetrics,
                                    apiMetrics = apiMetrics, // null for true FL
                                    allProbabilities = predictionResult.allProbabilities
                                )
                            )
                            
                            _uiState.value = FederatedLearningUiState.Success(
                                prediction = predictionResult.activity,
                                confidence = predictionResult.confidence / 100.0,
                                deviceMetrics = deviceMetrics,
                                apiMetrics = apiMetrics,
                                allProbabilities = predictionResult.allProbabilities,
                                showFeedback = true,
                                isOnDevice = true // TRUE FL: prediction ran on-device
                            )
                        } else {
                            _uiState.value = FederatedLearningUiState.Error("Model not loaded. Check model initialization.")
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = FederatedLearningUiState.Error("Prediction error: ${e.message}")
            }
        }
    }
    
    fun updateWeights(correctLabel: Int, activityName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = FederatedLearningUiState.Training
                
                Log.d("FLViewModel", "=== Starting Federated Learning Update ===")
                Log.d("FLViewModel", "Correct Label: $correctLabel ($activityName)")
                
                val sensorData = lastSensorData ?: run {
                    Log.e("FLViewModel", "No sensor data available!")
                    _uiState.value = FederatedLearningUiState.Error("No sensor data available")
                    return@launch
                }
                
                Log.d("FLViewModel", "Sensor data size: ${sensorData.size} features")
                
                // TRUE FEDERATED LEARNING: Compute gradients on-device
                Log.d("FLViewModel", "Computing gradients on-device...")
                val gradients = pytorchFLService.computeGradients(sensorData, correctLabel)
                
                if (gradients == null) {
                    Log.e("FLViewModel", "Failed to compute gradients")
                    _uiState.value = FederatedLearningUiState.Error("Failed to compute gradients on-device")
                    return@launch
                }
                
                Log.d("FLViewModel", "✅ Gradients computed: ${gradients.size} values")
                Log.d("FLViewModel", "Gradient range: [${gradients.minOrNull()}, ${gradients.maxOrNull()}]")
                
                lastGradients = gradients
                
                // Only send GRADIENTS to server (not raw data!)
                val deviceId = "android_${System.currentTimeMillis()}"
                val request = FLGradientsRequest(
                    gradients = gradients.toList(),
                    activity_name = activityName,
                    device_id = deviceId
                )
                
                Log.d("FLViewModel", "Sending gradients to server...")
                Log.d("FLViewModel", "Device ID: $deviceId")
                Log.d("FLViewModel", "Activity: $activityName")
                Log.d("FLViewModel", "Privacy: Raw data NOT sent, only gradients")
                
                val response = ApiService.api.flUpdateWeightsWithGradients(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    Log.d("FLViewModel", "✅ Server response: ${result.message}")
                    
                    if (result.gradients_applied != null && result.gradients_applied) {
                        Log.d("FLViewModel", "✅ Gradients applied to global model")
                    }
                    
                    _uiState.value = FederatedLearningUiState.TrainingSuccess(result.message)
                    Log.d("FLViewModel", "=== FL Update Complete ===")
                    

                } else {
                    Log.e("FLViewModel", "Server error: HTTP ${response.code()}")
                    _uiState.value = FederatedLearningUiState.Error("Training failed - Check internet connection")
                }
            } catch (e: Exception) {
                Log.e("FLViewModel", "=== FL Update Failed ===")
                Log.e("FLViewModel", "Error: ${e.message}", e)
                _uiState.value = FederatedLearningUiState.Error("Training error: ${e.message}")
            }
        }
    }
    
    fun confirmPrediction() {
        val currentState = _uiState.value
        if (currentState is FederatedLearningUiState.Success) {
            lastPredictionClass?.let { label ->
                updateWeights(label, currentState.prediction)
            }
        }
    }
    
    fun reset() {
        _uiState.value = FederatedLearningUiState.Idle
        lastSensorData = null
        lastPredictionClass = null
    }
    
    /**
     * NEW: Full FL flow with complete metrics tracking
     * Flow: Prediction -> Gradient Calc -> API Send -> Aggregation -> Model Update -> Complete
     * Tracks both device metrics and API metrics throughout the entire flow
     */
    fun startFullFLFlow() {
        viewModelScope.launch {
            try {
                if (!_isModelReady.value) {
                    _uiState.value = FederatedLearningUiState.Error("Model not ready. Please download the model first.")
                    return@launch
                }
                
                Log.d("FLViewModel", "=== Starting Full FL Flow ===")
                val flowStartTime = System.currentTimeMillis()
                
                _uiState.value = FederatedLearningUiState.Collecting
                
                // Capture initial metrics snapshot
                val startSnapshot = metricsCollector.captureSnapshot()
                
                sensorCollector.collectSensorData()
                    .take(1)
                    .collect { readings ->
                        // Step 1: Prediction
                        _uiState.value = FederatedLearningUiState.Predicting
                        val predictionStartTime = System.currentTimeMillis()
                        
                        val features = sensorCollector.processReadingsToFeatures(readings)
                        lastSensorData = features
                        
                        // On-device inference
                        val predictionResult = pytorchFLService.predict(features)
                        val predictionTime = System.currentTimeMillis() - predictionStartTime
                        Log.d("FLViewModel", "⏱️ Prediction time: ${predictionTime}ms")
                        
                        if (predictionResult == null) {
                            _uiState.value = FederatedLearningUiState.Error("Prediction failed")
                            return@collect
                        }
                        
                        lastPredictionClass = predictionResult.classIndex
                        
                        // Step 2: Compute Gradients
                        _uiState.value = FederatedLearningUiState.Training
                        val gradientStartTime = System.currentTimeMillis()
                        
                        Log.d("FLViewModel", "Computing gradients for class ${predictionResult.classIndex} (${predictionResult.activity})...")
                        val gradients = pytorchFLService.computeGradients(features, predictionResult.classIndex)
                        val gradientTime = System.currentTimeMillis() - gradientStartTime
                        Log.d("FLViewModel", "⏱️ Gradient computation time: ${gradientTime}ms")
                        
                        if (gradients == null) {
                            _uiState.value = FederatedLearningUiState.Error("Gradient computation failed")
                            return@collect
                        }
                        
                        lastGradients = gradients
                        val gradientNorm = sqrt(gradients.sumOf { it.toDouble() * it.toDouble() })
                        Log.d("FLViewModel", "✅ Gradients computed: ${gradients.size} values, norm: $gradientNorm")
                        
                        // Step 3: Send to API and get aggregation metrics
                        val apiSendStartTime = System.currentTimeMillis()
                        val deviceId = "android_${System.currentTimeMillis()}"
                        val request = FLGradientsRequest(
                            gradients = gradients.toList(),
                            activity_name = predictionResult.activity,
                            device_id = deviceId
                        )
                        
                        Log.d("FLViewModel", "Sending gradients to API for aggregation...")
                        val response = ApiService.api.flUpdateWeightsWithGradients(request)
                        val apiSendTime = System.currentTimeMillis() - apiSendStartTime
                        Log.d("FLViewModel", "⏱️ API communication time: ${apiSendTime}ms")
                        
                        if (!response.isSuccessful || response.body() == null) {
                            _uiState.value = FederatedLearningUiState.Error("API communication failed")
                            return@collect
                        }
                        
                        val result = response.body()!!
                        val aggregationInfo = result.aggregation_info
                        Log.d("FLViewModel", "✅ Server response: ${result.message}")
                        Log.d("FLViewModel", "Aggregation info: $aggregationInfo")
                        
                        // Step 4: Download and apply updated model
                        val modelUpdateStartTime = System.currentTimeMillis()
                        Log.d("FLViewModel", "Downloading updated model from server...")
                        
                        val modelUpdated = pytorchFLService.downloadModelFromServer()
                        if (modelUpdated) {
                            pytorchFLService.loadModel()
                            Log.d("FLViewModel", "✅ Model updated and reloaded")
                        }
                        
                        val modelUpdateTime = System.currentTimeMillis() - modelUpdateStartTime
                        Log.d("FLViewModel", "⏱️ Model update time: ${modelUpdateTime}ms")
                        
                        // Calculate total metrics
                        val totalTime = System.currentTimeMillis() - flowStartTime
                        val endSnapshot = metricsCollector.captureSnapshot()
                        val deviceMetrics = metricsCollector.calculateMetrics(startSnapshot, endSnapshot)
                        
                        // Create comprehensive flow metrics
                        val fullFlowMetrics = FullFlowMetrics(
                            prediction_time_ms = predictionTime,
                            gradient_calc_time_ms = gradientTime,
                            api_send_time_ms = apiSendTime,
                            api_aggregation_time_ms = 0, // Server doesn't report this yet
                            model_update_time_ms = modelUpdateTime,
                            total_time_ms = totalTime,
                            gradient_norm = gradientNorm,
                            aggregation_method = "federated_averaging"
                        )
                        
                        Log.d("FLViewModel", "=== Full FL Flow Complete ===")
                        Log.d("FLViewModel", "⏱️ Total time: ${totalTime}ms")
                        Log.d("FLViewModel", "  - Prediction: ${predictionTime}ms")
                        Log.d("FLViewModel", "  - Gradient calc: ${gradientTime}ms")
                        Log.d("FLViewModel", "  - API send: ${apiSendTime}ms")
                        Log.d("FLViewModel", "  - Model update: ${modelUpdateTime}ms")
                        
                        // Save to history with "Full FL" label
                        storageService.saveResult(
                            PredictionResult(
                                id = UUID.randomUUID().toString(),
                                activity = predictionResult.activity,
                                confidence = predictionResult.confidence / 100.0,
                                timestamp = System.currentTimeMillis(),
                                type = "Full FL", // NEW: Different type for comparison
                                deviceMetrics = deviceMetrics,
                                apiMetrics = null,
                                allProbabilities = predictionResult.allProbabilities,
                                fullFlowMetrics = fullFlowMetrics
                            )
                        )
                        
                        // Show Success state with prediction details and metrics
                        _uiState.value = FederatedLearningUiState.Success(
                            prediction = predictionResult.activity,
                            confidence = predictionResult.confidence / 100.0,
                            deviceMetrics = deviceMetrics,
                            apiMetrics = null,
                            allProbabilities = predictionResult.allProbabilities,
                            showFeedback = false, // Don't show feedback buttons for Full FL
                            isOnDevice = true,
                            fullFlowMetrics = fullFlowMetrics // Pass Full FL metrics
                        )
                    }
            } catch (e: Exception) {
                Log.e("FLViewModel", "Full FL flow error: ${e.message}", e)
                _uiState.value = FederatedLearningUiState.Error("Full FL error: ${e.message}")
            }
        }
    }
}
