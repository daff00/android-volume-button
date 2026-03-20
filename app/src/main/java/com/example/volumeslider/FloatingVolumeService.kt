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
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import kotlin.math.abs

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

    // The Quick Ball intellihide animation
    private val hideRunnable = Runnable {
        isCollapsed = true
        
        // Slide 75% off the screen depending on which side it's docked to
        val slideOutX = if (isDockedLeft) {
            -(floatingView.width * 0.75).toInt()
        } else {
            screenWidth - (floatingView.width * 0.25).toInt()
        }

        // Dim slightly and slide off screen
        floatingView.animate().alpha(0.6f).setDuration(300).start()
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

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
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
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        }
        
        btnDown.setOnClickListener {
            resetIdleTimer()
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
    }

    private fun setupDraggingAndSnapping() {
    // We'll use a tiny fixed value instead of the system default 
    // to make it feel much more sensitive.
    val moveThreshold = 5 

    floatingView.setOnTouchListener(object : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var isDragging = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 1. Instant Expand if hidden
                    if (isCollapsed) {
                        expandWidget()
                        return true 
                    }

                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false

                    resetIdleTimer()
                    
                    // We return false here so the buttons can still be "clicked" 
                    // if the user doesn't move their finger.
                    return false 
                }
                
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    // 2. Immediate Transition to Dragging
                    if (!isDragging && (abs(dx) > moveThreshold || abs(dy) > moveThreshold)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(floatingView, params)
                        
                        // IMPORTANT: Returning true here "steals" the touch from the buttons
                        return true 
                    }
                }
                
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        snapToEdge()
                        isDragging = false
                        return true // Consume the event so a button doesn't click on release
                    }
                }
            }
            return false
        }
    })
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
        val widgetCenter = params.x + (floatingView.width / 2)
        isDockedLeft = widgetCenter < (screenWidth / 2)

        // BUG FIX: Subtract floatingView.width so it doesn't fly off the right edge!
        val targetX = if (isDockedLeft) 0 else screenWidth - floatingView.width

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
        val channel = NotificationChannel(channelId, "Floating Volume", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
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
}