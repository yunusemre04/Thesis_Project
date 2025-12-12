package com.example.positiondeterminer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

data class PredictionResult(
    val id: String,
    val activity: String,
    val confidence: Double,
    val timestamp: Long,
    val type: String, // "DL", "FL", or "Full FL"
    val deviceMetrics: DeviceMetrics?,
    val apiMetrics: ApiMetrics? = null,
    val allProbabilities: Map<String, Double>? = null,
    val fullFlowMetrics: FullFlowMetrics? = null // NEW: Complete FL flow metrics
)

data class DeviceMetrics(
    val cpu_usage_percent: Double,
    val ram_usage_mb: Double,
    val duration_seconds: Double
)

// NEW: Comprehensive FL flow metrics
data class FullFlowMetrics(
    val prediction_time_ms: Long,
    val gradient_calc_time_ms: Long,
    val api_send_time_ms: Long,
    val api_aggregation_time_ms: Long,
    val model_update_time_ms: Long,
    val total_time_ms: Long,
    val gradient_norm: Double? = null,
    val aggregation_method: String? = null
)

class StorageService(private val context: Context) {
    private val gson = Gson()
    
    companion object {
        private val HISTORY_KEY = stringPreferencesKey("prediction_history")
    }
    
    fun getHistory(): Flow<List<PredictionResult>> = context.dataStore.data.map { preferences ->
        val json = preferences[HISTORY_KEY] ?: "[]"
        val type = object : TypeToken<List<PredictionResult>>() {}.type
        gson.fromJson(json, type)
    }
    
    suspend fun saveResult(result: PredictionResult) {
        context.dataStore.edit { preferences ->
            val currentHistoryJson = preferences[HISTORY_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<PredictionResult>>() {}.type
            val history: MutableList<PredictionResult> = gson.fromJson(currentHistoryJson, type)
            
            history.add(0, result) // Add to beginning
            
            // Keep only last 100 results
            if (history.size > 100) {
                history.removeAt(history.size - 1)
            }
            
            preferences[HISTORY_KEY] = gson.toJson(history)
        }
    }
    
    suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences[HISTORY_KEY] = "[]"
        }
    }
    
    suspend fun clearHistoryByType(type: String) {
        context.dataStore.edit { preferences ->
            val currentHistoryJson = preferences[HISTORY_KEY] ?: "[]"
            val listType = object : TypeToken<MutableList<PredictionResult>>() {}.type
            val history: MutableList<PredictionResult> = gson.fromJson(currentHistoryJson, listType)
            
            // Remove all results matching the specified type
            // For FL type, also remove "Full FL" entries
            history.removeAll { 
                if (type == "FL") {
                    it.type == "FL" || it.type == "Full FL"
                } else {
                    it.type == type
                }
            }
            
            preferences[HISTORY_KEY] = gson.toJson(history)
        }
    }
}
