package com.example.volumeslider

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    // We hold references to the views we need to interact with
    private lateinit var switchOverlay: Switch
    private lateinit var seekbarWidgetSize: SeekBar
    private lateinit var seekbarPeekWidth: SeekBar
    private lateinit var labelWidgetSize: TextView
    private lateinit var labelPeekWidth: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Wire up view references
        switchOverlay     = findViewById(R.id.switch_overlay)
        seekbarWidgetSize = findViewById(R.id.seekbar_widget_size)
        seekbarPeekWidth  = findViewById(R.id.seekbar_peek_width)
        labelWidgetSize   = findViewById(R.id.label_widget_size)
        labelPeekWidth    = findViewById(R.id.label_peek_width)

        // Load saved values and apply them to the UI controls
        loadSavedPreferences()

        // Set up listeners so changes are saved immediately
        setupListeners()
    }

    private fun loadSavedPreferences() {
        // Overlay toggle
        switchOverlay.isChecked = AppPreferences.isOverlayEnabled(this)

        // Widget size seekbar
        val savedSize = AppPreferences.getWidgetSizeDp(this)
        seekbarWidgetSize.progress = savedSize
        labelWidgetSize.text = "${savedSize}dp"

        // Peek width seekbar
        val savedPeek = AppPreferences.getPeekWidthDp(this)
        seekbarPeekWidth.progress = savedPeek
        labelPeekWidth.text = "${savedPeek}dp"
    }

    private fun setupListeners() {

        // ── Overlay toggle ──
        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setOverlayEnabled(this, isChecked)

            val intent = Intent(this, FloatingVolumeService::class.java)
            if (isChecked) {
                // User turned it ON — start the service
                startForegroundService(intent)
            } else {
                // User turned it OFF — stop the service
                stopService(intent)
            }
        }

        // ── Widget size seekbar ──
        seekbarWidgetSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update the label live as the user drags
                labelWidgetSize.text = "${progress}dp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Only save when the user LIFTS their finger.
                // Saving on every pixel of drag would spam SharedPreferences writes.
                val value = seekBar?.progress ?: AppPreferences.DEFAULT_WIDGET_SIZE
                AppPreferences.setWidgetSizeDp(this@SettingsActivity, value)
            }
        })

        // ── Peek width seekbar ──
        seekbarPeekWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                labelPeekWidth.text = "${progress}dp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val value = seekBar?.progress ?: AppPreferences.DEFAULT_PEEK_WIDTH
                AppPreferences.setPeekWidthDp(this@SettingsActivity, value)
            }
        })
    }
}