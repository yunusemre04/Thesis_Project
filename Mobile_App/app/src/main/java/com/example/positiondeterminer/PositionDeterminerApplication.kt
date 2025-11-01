package com.example.positiondeterminer

import android.app.Application
import com.facebook.soloader.SoLoader

/**
 * Application class for global initialization
 */
class PositionDeterminerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize SoLoader for PyTorch native libraries
        SoLoader.init(this, false)
    }
}
