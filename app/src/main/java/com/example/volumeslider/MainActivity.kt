package com.example.volumeslider

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class MainActivity : Activity() {
    
    // An arbitrary number to identify our permission request
    private val OVERLAY_PERMISSION_REQ_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Check if we already have permission to draw the floating UI
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow 'Display over other apps'", Toast.LENGTH_LONG).show()
            // 2. If no permission, open the specific Android settings screen for our app
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            // 3. If we DO have permission, start the floating volume service immediately!
            startFloatingService()
        }
    }

    // This runs when the user comes back from the Settings screen
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
            } else {
                Toast.makeText(this, "Permission denied. App cannot function.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startFloatingService() {
        // Prepare the intent to start our background engine
        val intent = Intent(this, FloatingVolumeService::class.java)
        
        // Android 8+ requires us to use startForegroundService for long-running background tasks
        startForegroundService(intent)
        
        // Close this setup Activity so the user just sees the floating UI
        finish() 
    }
}
