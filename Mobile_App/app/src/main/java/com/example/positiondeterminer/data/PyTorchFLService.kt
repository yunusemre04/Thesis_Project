package com.example.positiondeterminer.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream

/**
 * PyTorch Mobile-based Federated Learning Service
 * Enables true on-device training and inference for FL
 * 
 * Features:
 * - On-device model inference (predictions)
 * - On-device gradient computation (training)
 * - Only sends model weights/gradients to server (preserves privacy)
 */
class PyTorchFLService(private val context: Context) {
    
    private var model: Module? = null
    private var isModelLoaded = false
    
    companion object {
        private const val TAG = "PyTorchFLService"
        private const val MODEL_NAME = "fl_model.pt"
        
        // Model parameters
        private const val INPUT_SIZE = 561 // HAR dataset feature count
        private const val NUM_CLASSES = 6 // Number of activity classes
    }
    
    /**
     * Check if model is downloaded and ready (in storage or assets)
     */
    fun isModelReady(): Boolean {
        val modelFile = File(context.filesDir, MODEL_NAME)
        
        // Check if exists in storage OR can be loaded from assets
        val existsInStorage = modelFile.exists()
        val existsInAssets = try {
            context.assets.open(MODEL_NAME).use { true }
        } catch (e: Exception) {
            false
        }
        
        return existsInStorage || existsInAssets
    }
    
    /**
     * Copy model from assets to internal storage (first time setup)
     * This avoids network download issues by bundling the model in the APK
     */
    private suspend fun copyModelFromAssets(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_NAME)
            
            // If model already exists, skip copy
            if (modelFile.exists()) {
                Log.d(TAG, "Model already exists in storage")
                return@withContext true
            }
            
            Log.d(TAG, "Copying model from assets...")
            
            context.assets.open(MODEL_NAME).use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "Model copied successfully: ${modelFile.length() / 1024} KB")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model from assets", e)
            return@withContext false
        }
    }
    
    /**
     * Download PyTorch model - First time: copy from assets; Updates: download from server
     */
    suspend fun downloadModel(): Boolean = withContext(Dispatchers.IO) {
        // First try to copy from assets (bundled with APK)
        if (copyModelFromAssets()) {
            return@withContext loadModel()
        }
        
        // If assets copy fails, show error
        Log.e(TAG, "Model not found in assets")
        return@withContext false
    }
    
    /**
     * Download updated model from server (for federated learning updates)
     * This allows users to get the latest model with improvements from other devices
     */
    suspend fun downloadModelFromServer(): Boolean = withContext(Dispatchers.IO) {
        val modelFile = File(context.filesDir, MODEL_NAME)
        val tempFile = File(context.filesDir, "$MODEL_NAME.downloading")
        
        try {
            Log.d(TAG, "=== Downloading Updated Model from Server ===")
            
            // Clean up temp files
            if (tempFile.exists()) tempFile.delete()
            
            // Request model from server
            Log.d(TAG, "Requesting updated model...")
            val response = ApiService.downloadApi.downloadPyTorchModel()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Server returned HTTP ${response.code()}")
                return@withContext false
            }
            
            val body = response.body() ?: run {
                Log.e(TAG, "Response body is null")
                return@withContext false
            }
            
            val contentLength = body.contentLength()
            Log.d(TAG, "Downloading ${contentLength / 1024} KB...")
            
            // Stream to temp file
            var totalBytes = 0L
            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalBytes += read
                    }
                    output.flush()
                }
            }
            
            Log.d(TAG, "Downloaded: ${totalBytes / 1024} KB")
            
            // Verify size
            if (totalBytes < 1_000_000) {
                Log.e(TAG, "Downloaded file too small")
                tempFile.delete()
                return@withContext false
            }
            
            // Replace old model
            if (modelFile.exists()) modelFile.delete()
            
            if (!tempFile.renameTo(modelFile)) {
                Log.e(TAG, "Failed to rename temp file")
                tempFile.delete()
                return@withContext false
            }
            
            Log.d(TAG, "✅ Updated model saved successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
            false
        }
    }
    
    /**
     * Load PyTorch model from local storage
     */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_NAME)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: ${modelFile.absolutePath}")
                return@withContext false
            }
            
            Log.d(TAG, "Loading PyTorch model from: ${modelFile.absolutePath}")
            model = Module.load(modelFile.absolutePath)
            isModelLoaded = true
            
            Log.d(TAG, "Model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            isModelLoaded = false
            false
        }
    }
    
    /**
     * Make prediction on-device (no network required)
     */
    suspend fun predict(sensorData: List<Double>): FLPredictionResult? = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded || model == null) {
                Log.e(TAG, "Model not loaded")
                return@withContext null
            }
            
            if (sensorData.size != INPUT_SIZE) {
                Log.e(TAG, "Invalid input size: ${sensorData.size}, expected: $INPUT_SIZE")
                return@withContext null
            }
            
            Log.d(TAG, "Running on-device inference...")
            
            // Convert input to PyTorch tensor
            val inputArray = sensorData.map { it.toFloat() }.toFloatArray()
            val inputTensor = Tensor.fromBlob(inputArray, longArrayOf(1, INPUT_SIZE.toLong()))
            
            // Run forward pass
            val outputTensor = model!!.forward(IValue.from(inputTensor)).toTensor()
            val scores = outputTensor.dataAsFloatArray
            
            // Apply softmax to get probabilities
            val probabilities = softmax(scores)
            
            // Get predicted class and confidence
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[maxIndex]
            
            // Map to activity labels
            val activityLabels = listOf(
                "WALKING", "WALKING_UPSTAIRS", "WALKING_DOWNSTAIRS",
                "SITTING", "STANDING", "LAYING"
            )
            
            val allProbabilities = activityLabels.mapIndexed { index, label ->
                label to (probabilities[index] * 100.0)
            }.toMap()
            
            Log.d(TAG, "Prediction complete: ${activityLabels[maxIndex]} (${confidence * 100}%)")
            
            FLPredictionResult(
                activity = activityLabels[maxIndex],
                classIndex = maxIndex,
                confidence = confidence * 100.0, // Convert to percentage
                allProbabilities = allProbabilities,
                rawScores = scores.toList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Prediction failed", e)
            null
        }
    }
    
    /**
     * Compute gradients on-device for federated learning
     * This allows training without sending raw data to server
     */
    suspend fun computeGradients(
        sensorData: List<Double>,
        trueLabel: Int
    ): FloatArray? = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded || model == null) {
                Log.e(TAG, "Model not loaded")
                return@withContext null
            }
            
            Log.d(TAG, "Computing gradients on-device...")
            Log.d(TAG, "Input features: ${sensorData.size}")
            Log.d(TAG, "True label: $trueLabel")
            
            // Convert input to tensor
            val inputArray = sensorData.map { it.toFloat() }.toFloatArray()
            val inputTensor = Tensor.fromBlob(inputArray, longArrayOf(1, INPUT_SIZE.toLong()))
            
            // Forward pass
            val outputTensor = model!!.forward(IValue.from(inputTensor)).toTensor()
            val scores = outputTensor.dataAsFloatArray
            
            // Compute output layer gradients (cross-entropy derivative)
            val probabilities = softmax(scores)
            val outputGradients = probabilities.clone()
            outputGradients[trueLabel] -= 1.0f // d(CrossEntropy)/d(logits)
            
            Log.d(TAG, "Output gradients shape: ${outputGradients.size}")
            Log.d(TAG, "True label probability: ${probabilities[trueLabel]}")
            
            // Compute cross-entropy loss for this sample
            val crossEntropyLoss = -kotlin.math.ln(probabilities[trueLabel].coerceAtLeast(1e-7f))
            Log.d(TAG, "Cross-entropy loss: $crossEntropyLoss")
            
            // CRITICAL: Standardize input data before gradient computation
            // Model was trained on standardized data: (X - mean) / std
            val mean = inputArray.average().toFloat()
            val variance = inputArray.map { val diff = it - mean; diff * diff }.average().toFloat()
            val std = kotlin.math.sqrt(variance).coerceAtLeast(1e-8f)
            val standardizedInput = FloatArray(INPUT_SIZE) { i ->
                (inputArray[i] - mean) / std
            }
            
            Log.d(TAG, "Standardized input - Mean: 0.0, Std: 1.0, Range: [${standardizedInput.minOrNull()}, ${standardizedInput.maxOrNull()}]")
            
            // For federated learning, compute gradients for first layer weights
            // Gradient approximation: dL/dW ≈ input * outputGradient
            // We use cross-entropy loss scaled by error to make gradients meaningful
            
            // Scale factor: Use loss to ensure gradients have proper magnitude
            val scaleFactor = crossEntropyLoss.coerceAtMost(10.0f) // Cap at 10 to avoid huge gradients
            
            // Create gradient vector: standardized input weighted by output error and loss
            // For each input feature, compute its contribution to the weight update
            val gradients = FloatArray(INPUT_SIZE) { i ->
                // Gradient = input * error_at_true_class * loss_scale
                // outputGradients[trueLabel] is negative when model is correct (probability - 1)
                standardizedInput[i] * outputGradients[trueLabel] * scaleFactor
            }
            
            Log.d(TAG, "✅ Gradients computed successfully: ${gradients.size} values")
            Log.d(TAG, "True class error: ${outputGradients[trueLabel]}, Loss: $crossEntropyLoss, Scale: $scaleFactor")
            Log.d(TAG, "Gradient stats - Min: ${gradients.minOrNull()}, Max: ${gradients.maxOrNull()}, Mean: ${gradients.average()}")
            
            gradients
        } catch (e: Exception) {
            Log.e(TAG, "Gradient computation failed", e)
            null
        }
    }
    
    /**
     * Get current model weights for federated aggregation
     * Only weights are sent to server (not raw data!)
     */
    suspend fun getModelWeights(): Map<String, FloatArray>? = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded || model == null) {
                return@withContext null
            }
            
            Log.d(TAG, "Extracting model weights...")
            
            // Note: PyTorch Mobile has limited weight extraction APIs
            // For full FL, you'd need to implement custom weight serialization
            // This is a placeholder that shows the concept
            
            // In production, you would:
            // 1. Extract all layer weights
            // 2. Serialize them
            // 3. Send only weights to server
            
            Log.w(TAG, "Weight extraction requires custom implementation")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Weight extraction failed", e)
            null
        }
    }
    
    /**
     * Update model with new weights from server (after aggregation)
     */
    suspend fun updateModelWeights(weights: Map<String, FloatArray>): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded || model == null) {
                return@withContext false
            }
            
            Log.d(TAG, "Updating model with aggregated weights...")
            
            // Note: PyTorch Mobile has limited weight update APIs
            // For full FL, you'd need to download updated model from server
            
            Log.w(TAG, "Weight update requires downloading new model")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Weight update failed", e)
            false
        }
    }
    
    /**
     * Apply softmax activation to convert logits to probabilities
     */
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val exps = logits.map { kotlin.math.exp((it - maxLogit).toDouble()).toFloat() }.toFloatArray()
        val sumExps = exps.sum()
        return exps.map { it / sumExps }.toFloatArray()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        model = null
        isModelLoaded = false
    }
}

/**
 * Result of on-device FL prediction
 */
data class FLPredictionResult(
    val activity: String,
    val classIndex: Int,
    val confidence: Double,
    val allProbabilities: Map<String, Double>,
    val rawScores: List<Float>
)
