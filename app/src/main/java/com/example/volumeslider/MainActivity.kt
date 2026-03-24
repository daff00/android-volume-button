package com.example.volumeslider

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class MainActivity : Activity() {

    private val OVERLAY_PERMISSION_REQ_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow 'Display over other apps'", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            // Permission already granted — go straight to Settings screen
            openSettingsScreen()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Settings.canDrawOverlays(this)) {
                openSettingsScreen()
            } else {
                Toast.makeText(this, "Permission denied. App cannot function.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun openSettingsScreen() {
        startActivity(Intent(this, SettingsActivity::class.java))
        // Don't call finish() here — the user should be able to
        // press back from Settings and not land on a blank screen.
        // MainActivity will just sit in the back stack behind Settings.
        finish()
    }
}