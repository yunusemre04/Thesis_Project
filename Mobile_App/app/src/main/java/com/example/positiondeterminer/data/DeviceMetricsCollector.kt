package com.example.positiondeterminer.data

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import android.util.Log
import java.io.File

class DeviceMetricsCollector(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val TAG = "DeviceMetricsCollector"
    
    data class MetricsSnapshot(
        val timestamp: Long,
        val threadCpuTimeNanos: Long,
        val uptimeMillis: Long,
        val ramUsageMB: Double
    )
    
    /**
     * Captures current CPU and RAM usage using real Android APIs
     */
    fun captureSnapshot(): MetricsSnapshot {
        // Use Debug.threadCpuTimeNanos() - real thread CPU time
        val threadCpuTime = Debug.threadCpuTimeNanos()
        val uptime = android.os.SystemClock.uptimeMillis()
        val ramUsage = getRamUsage()
        
        Log.d(TAG, "Snapshot - ThreadCPU: ${threadCpuTime / 1_000_000}ms, Uptime: ${uptime}ms, RAM: $ramUsage MB")
        
        return MetricsSnapshot(
            timestamp = System.currentTimeMillis(),
            threadCpuTimeNanos = threadCpuTime,
            uptimeMillis = uptime,
            ramUsageMB = ramUsage
        )
    }
    
    /**
     * Get current app RAM usage in MB using ActivityManager
     * Uses PSS (Proportional Set Size) - the standard metric for Android memory measurement
     * PSS = Private memory + (Shared memory / number of processes sharing it)
     * This is the official metric recommended by Google for app memory profiling
     */
    private fun getRamUsage(): Double {
        return try {
            val pids = intArrayOf(Process.myPid())
            val processMemInfo = activityManager.getProcessMemoryInfo(pids)
            
            if (processMemInfo.isNotEmpty()) {
                val memInfo = processMemInfo[0]
                
                // totalPss = Proportional Set Size in KB
                val totalPss = memInfo.totalPss
                val totalPssMB = totalPss / 1024.0
                
                // Log detailed breakdown for thesis verification
                Log.d(TAG, "RAM Details - Total PSS: $totalPssMB MB, " +
                        "Private Dirty: ${memInfo.totalPrivateDirty / 1024.0} MB, " +
                        "Shared Dirty: ${memInfo.totalSharedDirty / 1024.0} MB")
                
                totalPssMB
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading RAM usage: ${e.message}")
            0.0
        }
    }
    
    /**
     * Calculate real metrics between two snapshots
     */
    fun calculateMetrics(startSnapshot: MetricsSnapshot, endSnapshot: MetricsSnapshot): DeviceMetrics {
        val durationSeconds = (endSnapshot.timestamp - startSnapshot.timestamp) / 1000.0
        
        // Real CPU time spent by this thread in nanoseconds
        val cpuTimeNanos = endSnapshot.threadCpuTimeNanos - startSnapshot.threadCpuTimeNanos
        val cpuTimeSeconds = cpuTimeNanos / 1_000_000_000.0
        
        // Real wall clock time in seconds
        val wallTimeSeconds = (endSnapshot.uptimeMillis - startSnapshot.uptimeMillis) / 1000.0
        
        Log.d(TAG, "CPU Time: ${cpuTimeSeconds}s, Wall Time: ${wallTimeSeconds}s")
        
        // Real CPU usage: (CPU time / Wall time) * 100
        // This gives actual percentage of time CPU was working on this thread
        val cpuUsagePercent = if (wallTimeSeconds > 0) {
            (cpuTimeSeconds / wallTimeSeconds) * 100.0
        } else {
            0.0
        }
        
        // Real RAM usage: Use end snapshot (memory after operation)
        // This shows the actual memory footprint at operation completion
        // More meaningful than delta since operations can be memory-efficient
        val ramUsageMB = endSnapshot.ramUsageMB
        val ramDelta = endSnapshot.ramUsageMB - startSnapshot.ramUsageMB
        
        Log.d(TAG, "Real Metrics - CPU: $cpuUsagePercent%, RAM: $ramUsageMB MB (Delta: ${String.format("%.2f", ramDelta)} MB), Duration: ${durationSeconds}s")
        
        return DeviceMetrics(
            cpu_usage_percent = cpuUsagePercent,
            ram_usage_mb = ramUsageMB,
            duration_seconds = durationSeconds
        )
    }
}
