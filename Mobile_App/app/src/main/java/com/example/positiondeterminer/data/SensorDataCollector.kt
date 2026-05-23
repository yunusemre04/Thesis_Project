package com.example.positiondeterminer.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorDataCollector(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    companion object {

        const val COLLECTION_DURATION_MS = 2560 // 2.56 seconds
        const val UPDATE_INTERVAL_US = 20000 // 20ms = 50Hz (1000000/50)
    }
    
    data class SensorReading(
        val accelerometer: Triple<Float, Float, Float>,
        val gyroscope: Triple<Float, Float, Float>,
        val timestamp: Long
    )
    
    fun collectSensorData(): Flow<List<SensorReading>> = callbackFlow {
        val readings = mutableListOf<SensorReading>()
        var accelData: Triple<Float, Float, Float>? = null
        var gyroData: Triple<Float, Float, Float>? = null
        var startTime = System.currentTimeMillis()
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            accelData = Triple(it.values[0], it.values[1], it.values[2])
                        }
                        Sensor.TYPE_GYROSCOPE -> {
                            gyroData = Triple(it.values[0], it.values[1], it.values[2])
                        }
                    }
                    
                    // Only add reading when we have both sensor types
                    // CRITICAL FIX: Create NEW Triple objects to avoid reference reuse
                    if (accelData != null && gyroData != null) {
                        readings.add(
                            SensorReading(
                                accelerometer = Triple(accelData.first, accelData.second, accelData.third),
                                gyroscope = Triple(gyroData.first, gyroData.second, gyroData.third),
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        
                        // Check if we've collected enough data
                        if (System.currentTimeMillis() - startTime >= COLLECTION_DURATION_MS) {
                            trySend(readings.toList())
                            readings.clear()
                            startTime = System.currentTimeMillis()
                        }
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        // Register listeners
        accelerometer?.let {
            sensorManager.registerListener(listener, it, UPDATE_INTERVAL_US)
        }
        gyroscope?.let {
            sensorManager.registerListener(listener, it, UPDATE_INTERVAL_US)
        }
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    /**
     * Temporal Similarity-Based Data Filter Component:
     * Stores the previous sliding window's extracted feature vector.
     */
    private var previousFeatures: DoubleArray? = null

    /**
     * Computes the Cosine Similarity between two feature vectors to detect redundant sedentary states.
     */
    private fun computeCosineSimilarity(v1: DoubleArray, v2: DoubleArray): Double {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0.0
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        if (normA == 0.0 || normB == 0.0) return 0.0
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))
    }

    /**
     * Process sensor readings into the format expected by the ML model
     * Returns a flat array of 561 UCI HAR compatible features or an empty list if data is dropped.
     */
    fun processReadingsToFeatures(readings: List<SensorReading>): List<Double> {
        android.util.Log.d("SensorDataCollector", "🔄 Processing ${readings.size} readings to features")
        
        if (readings.isEmpty()) {
            android.util.Log.w("SensorDataCollector", "⚠️ No readings to process!")
            return emptyList()
        }
        
        // Log sample of readings
        if (readings.isNotEmpty()) {
            val first = readings.first()
            val last = readings.last()
            android.util.Log.d("SensorDataCollector", "  First reading - Accel: ${first.accelerometer}, Gyro: ${first.gyroscope}")
            android.util.Log.d("SensorDataCollector", "  Last reading - Accel: ${last.accelerometer}, Gyro: ${last.gyroscope}")
        }

        // Use proper UCI HAR feature extraction
        val extractor = FeatureExtractor()
        val features = extractor.extractFeatures(readings)
        
        // --- Temporal Similarity-Based Data Filter ---
        // Compute Cosine Similarity between current feature vector (v_t) and previous (v_t-1)
        val currentFeaturesArray = features.toDoubleArray()
        previousFeatures?.let { prev ->
            val similarity = computeCosineSimilarity(currentFeaturesArray, prev)
            
            // If similarity > 0.98, the frame is extremely redundant (e.g., prolonged sedentary state like sitting/laying).
            // We immediately drop the frame from the local training buffer by returning empty, 
            // slashing local training computational overhead and drastically saving battery.
            if (similarity > 0.98) {
                android.util.Log.d("SensorDataCollector", "🛑 TEMPORAL FILTER: Dropping redundant frame! Cosine Similarity: $similarity > 0.98")
                return emptyList()
            }
        }
        
        // Update previous features buffer for next comparison window
        previousFeatures = currentFeaturesArray
        
        android.util.Log.d("SensorDataCollector", "✅ Generated ${features.size} features")
        return features
    }
    
   
}
