package com.example.volumeslider

import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat

class FloatingVolumeService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var audioManager: AudioManager
    private lateinit var params: WindowManager.LayoutParams

    // State trackers for our new Quick Ball feature
    private var isCollapsed = false
    private var isDockedLeft = true
    private var screenWidth = 0

    private val handler = Handler(Looper.getMainLooper())
    private val idleTimeout = 3000L // 3 seconds before hiding

    // Add this helper function inside your class
    private fun updateDisplayMetrics() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val bounds = wm.currentWindowMetrics.bounds
        screenWidth = bounds.width()
    }

    // The Quick Ball intellihide animation
    private val hideRunnable = Runnable {
        updateDisplayMetrics()
        isCollapsed = true

        val viewWidth = floatingView.width
        // Increased from 15dp to 32dp. Still unobtrusive, but
        // actually hittable — especially important on a 6.5" display.
        val peekDp = 32
        val peekPx = (peekDp * resources.displayMetrics.density).toInt()

        val slideOutX =
                if (isDockedLeft) {
                    -viewWidth + peekPx
                } else {
                    screenWidth - peekPx
                }

        // REMOVED scaleX/scaleY: scaling the window overlay desyncs the
        // visual bounds from the touch hitbox in WindowManager. Opacity
        // only — the widget fades but its touchable area stays accurate.
        floatingView.animate().alpha(0.4f).setDuration(300).start()

        animateWidgetPosition(params.x, slideOutX, 300)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        screenWidth = resources.displayMetrics.widthPixels // Get accurate screen width

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_control, null)

        params =
                WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        setupButtons()
        setupDraggingAndSnapping()

        windowManager.addView(floatingView, params)

        // Use a short post to wait for the view to measure its width before snapping
        floatingView.post { snapToEdge() }
    }

    private fun setupButtons() {
        val btnUp = floatingView.findViewById<Button>(R.id.btn_vol_up)
        val btnDown = floatingView.findViewById<Button>(R.id.btn_vol_down)

        btnUp.setOnClickListener {
            resetIdleTimer()
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
            )
        }

        btnDown.setOnClickListener {
            resetIdleTimer()
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
            )
        }
    }

    private fun setupDraggingAndSnapping() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isCollapsed) {
                        expandWidget()
                        return@setOnTouchListener true
                    }
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    resetIdleTimer()
                    false // Let children handle taps normally
                }
                MotionEvent.ACTION_MOVE -> {
                    isDragging = true
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        isDragging = false
                        snapToEdge()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    // Restores the widget to full size and position
    private fun expandWidget() {
        isCollapsed = false
        floatingView.animate().alpha(1.0f).setDuration(200).start()

        val targetX = if (isDockedLeft) 0 else screenWidth - floatingView.width
        animateWidgetPosition(params.x, targetX, 200)
        resetIdleTimer()
    }

    private fun snapToEdge() {
        updateDisplayMetrics() // Refresh width before snapping

        val viewWidth = floatingView.width
        val widgetCenter = params.x + (viewWidth / 2)
        isDockedLeft = widgetCenter < (screenWidth / 2)

        val targetX = if (isDockedLeft) 0 else screenWidth - viewWidth

        // Keep it within vertical bounds so it doesn't get "stuck" off-screen
        val screenHeight = resources.displayMetrics.heightPixels
        if (params.y < 0) params.y = 0
        if (params.y > screenHeight - floatingView.height) {
            params.y = screenHeight - floatingView.height
        }

        animateWidgetPosition(params.x, targetX, 200)
        resetIdleTimer()
    }

    // Helper function to keep our animations clean
    private fun animateWidgetPosition(startX: Int, endX: Int, durationMs: Long) {
        val animator = ValueAnimator.ofInt(startX, endX)
        animator.duration = durationMs
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            windowManager.updateViewLayout(floatingView, params)
        }
        animator.start()
    }

    private fun resetIdleTimer() {
        if (!isCollapsed) {
            floatingView.alpha = 1.0f
        }
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, idleTimeout)
    }

    private fun startForegroundNotification() {
        val channelId = "VolumeServiceChannel"
        val manager = getSystemService(NotificationManager::class.java)
        val channel =
                NotificationChannel(
                        channelId,
                        "Floating Volume",
                        NotificationManager.IMPORTANCE_LOW
                )
        manager.createNotificationChannel(channel)

        val notification =
                NotificationCompat.Builder(this, channelId)
                        .setContentTitle("Volume Overlay Active")
                        .setContentText("The volume slider is running.")
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(hideRunnable)
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // postOnAnimation waits for exactly one layout pass to complete.
        // This is guaranteed to be after the display metrics are updated,
        // unlike an arbitrary postDelayed(100) which is just a guess.
        floatingView.postOnAnimation {
            updateDisplayMetrics()
            // Clamp Y position in case the screen got shorter (landscape).
            val screenHeight = resources.displayMetrics.heightPixels
            params.y = params.y.coerceIn(0, screenHeight - floatingView.height)
            windowManager.updateViewLayout(floatingView, params)
            snapToEdge()
        }
    }
}
