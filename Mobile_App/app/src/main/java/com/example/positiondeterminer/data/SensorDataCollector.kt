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
        const val SAMPLE_RATE_HZ = 50 // 50 Hz like UCI HAR dataset
        const val WINDOW_SIZE = 128 // 128 samples per window
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
                    if (accelData != null && gyroData != null) {
                        readings.add(
                            SensorReading(
                                accelerometer = accelData!!,
                                gyroscope = gyroData!!,
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
     * Process sensor readings into the format expected by the ML model
     * Returns a flat array of 561 features
     */
    fun processReadingsToFeatures(readings: List<SensorReading>): List<Double> {
        if (readings.isEmpty()) return List(561) { 0.0 }
        
        // Extract accelerometer and gyroscope data
        val accelX = readings.map { it.accelerometer.first.toDouble() }
        val accelY = readings.map { it.accelerometer.second.toDouble() }
        val accelZ = readings.map { it.accelerometer.third.toDouble() }
        val gyroX = readings.map { it.gyroscope.first.toDouble() }
        val gyroY = readings.map { it.gyroscope.second.toDouble() }
        val gyroZ = readings.map { it.gyroscope.third.toDouble() }
        
        // Simple feature extraction (mean, std, min, max for each axis)
        val features = mutableListOf<Double>()
        
        listOf(accelX, accelY, accelZ, gyroX, gyroY, gyroZ).forEach { data ->
            features.add(data.average()) // mean
            features.add(calculateStd(data)) // std
            features.add(data.minOrNull() ?: 0.0) // min
            features.add(data.maxOrNull() ?: 0.0) // max
        }
        
        // Pad to 561 features if necessary
        while (features.size < 561) {
            features.add(0.0)
        }
        
        return features.take(561)
    }
    
    private fun calculateStd(data: List<Double>): Double {
        val mean = data.average()
        val variance = data.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}
