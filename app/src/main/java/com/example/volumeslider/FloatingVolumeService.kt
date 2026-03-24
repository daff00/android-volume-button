package com.example.volumeslider

import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
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
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat

class FloatingVolumeService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var audioManager: AudioManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var draggableLayout: DraggableLayout

    // The two visual states
    private lateinit var peekTab: FrameLayout
    private lateinit var menuPanel: LinearLayout

    private var isCollapsed = false
    private var isDockedLeft = true
    private var screenWidth = 0
    private var isMuted = false

    private var currentWidgetSizePx = 0
    private var currentPeekWidthPx = 0

    private val handler = Handler(Looper.getMainLooper())
    private val idleTimeout = 3000L
    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    private val deepDimRunnable = Runnable {
        if (isCollapsed) {
            floatingView.animate().alpha(0.2f).setDuration(400).start()
        }
    }

    private val hideRunnable = Runnable { collapseTopeek() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        loadPreferences()

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_control, null)

        // Get references to all the views we'll control
        draggableLayout = floatingView as DraggableLayout
        peekTab = floatingView.findViewById(R.id.peek_tab)
        menuPanel = floatingView.findViewById(R.id.menu_panel)

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
        params.y = 200

        setupButtons()
        setupDragging()
        registerPrefListener()

        windowManager.addView(floatingView, params)

        floatingView.post {
            applyWidgetSize(currentWidgetSizePx)
            snapToEdge()
        }
    }

    // ── Preferences ───────────────────────────────────────────────────────────

    private fun loadPreferences() {
        val density = resources.displayMetrics.density
        currentWidgetSizePx = (AppPreferences.getWidgetSizeDp(this) * density).toInt()
        currentPeekWidthPx = (AppPreferences.getPeekWidthDp(this) * density).toInt()
    }

    private fun registerPrefListener() {
        prefListener =
                AppPreferences.registerListener(this) { key ->
                    when (key) {
                        AppPreferences.KEY_WIDGET_SIZE -> {
                            val density = resources.displayMetrics.density
                            currentWidgetSizePx =
                                    (AppPreferences.getWidgetSizeDp(this) * density).toInt()
                            applyWidgetSize(currentWidgetSizePx)
                        }
                        AppPreferences.KEY_PEEK_WIDTH -> {
                            val density = resources.displayMetrics.density
                            currentPeekWidthPx =
                                    (AppPreferences.getPeekWidthDp(this) * density).toInt()
                            if (isCollapsed) {
                                handler.removeCallbacks(hideRunnable)
                                collapseTopeek()
                            }
                        }
                    }
                }
    }

    private fun applyWidgetSize(sizePx: Int) {
        val btnUp = floatingView.findViewById<ImageButton>(R.id.btn_vol_up)
        val btnDown = floatingView.findViewById<ImageButton>(R.id.btn_vol_down)

        listOf(btnUp, btnDown).forEach { btn ->
            val lp = btn.layoutParams
            lp.width = sizePx
            lp.height = sizePx
            btn.layoutParams = lp
        }

        floatingView.requestLayout()
        floatingView.post { snapToEdge() }
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private fun setupButtons() {
        floatingView.findViewById<ImageButton>(R.id.btn_vol_up).setOnClickListener {
            resetIdleTimer()
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
            )
        }

        floatingView.findViewById<ImageButton>(R.id.btn_vol_down).setOnClickListener {
            resetIdleTimer()
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
            )
        }

        floatingView.findViewById<Button>(R.id.btn_mute).apply {
            setOnClickListener {
                resetIdleTimer()
                toggleMute()
            }
        }

        // Tapping the peek tab expands the widget
        peekTab.setOnClickListener { expandToMenu() }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        val btnMute = floatingView.findViewById<Button>(R.id.btn_mute)

        if (isMuted) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            btnMute.text = "UNMUTE"
            btnMute.alpha = 1.0f
        } else {
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    0
            )
            btnMute.text = "MUTE"
            btnMute.alpha = 0.6f
        }
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    private fun setupDragging() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    // Don't reset idle timer here when collapsed —
                    // we only do that on confirmed tap (ACTION_UP without drag)
                    if (!isCollapsed) resetIdleTimer()
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(floatingView, params)
                    // Keep suppressing idle timer during drag
                    handler.removeCallbacks(hideRunnable)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        isDragging = false
                        // snapToEdge() updates isDockedLeft correctly
                        // for both expanded and collapsed states
                        snapToEdge()
                        true
                    } else {
                        // Clean tap — if collapsed, expand
                        if (isCollapsed) {
                            expandToMenu()
                            true
                        } else {
                            false
                        }
                    }
                }
                else -> false
            }
        }
    }

    // ── Expand / Collapse ─────────────────────────────────────────────────────

    private fun collapseTopeek() {
        isCollapsed = true
        draggableLayout.isDragEnabled = true

        menuPanel.visibility = View.GONE
        peekTab.visibility = View.VISIBLE

        updateDockedSide()

        // Flip the peek tab shape to always face inward toward the screen
        peekTab.setBackgroundResource(
                if (isDockedLeft) R.drawable.bg_peek_tab else R.drawable.bg_peek_tab_right
        )

        floatingView.post {
            val targetX = if (isDockedLeft) 0 else screenWidth - currentPeekWidthPx
            floatingView.animate().alpha(0.6f).setDuration(250).start()
            animateWidgetPosition(params.x, targetX, 280)
            handler.removeCallbacks(deepDimRunnable)
            handler.postDelayed(deepDimRunnable, 2000L)
        }
    }

    private fun expandToMenu() {
        isCollapsed = false
        draggableLayout.isDragEnabled = true

        // Cancel deep dim if it was scheduled or already applied
        handler.removeCallbacks(deepDimRunnable)

        peekTab.visibility = View.GONE
        menuPanel.visibility = View.VISIBLE

        floatingView.post {
            val targetX = if (isDockedLeft) 0 else screenWidth - floatingView.width
            floatingView.animate().alpha(1.0f).setDuration(200).start()
            animateWidgetPosition(params.x, targetX, 220)
        }

        resetIdleTimer()
    }

    // ── Edge snapping ─────────────────────────────────────────────────────────

    private fun snapToEdge() {
        updateDisplayMetrics()
        updateDockedSide()

        // Keep peek tab shape in sync when snapping
        if (isCollapsed) {
            peekTab.setBackgroundResource(
                    if (isDockedLeft) R.drawable.bg_peek_tab else R.drawable.bg_peek_tab_right
            )
        }

        val screenHeight = resources.displayMetrics.heightPixels
        params.y = params.y.coerceIn(0, screenHeight - floatingView.height)

        val targetX =
                if (isCollapsed) {
                    if (isDockedLeft) 0 else screenWidth - currentPeekWidthPx
                } else {
                    if (isDockedLeft) 0 else screenWidth - floatingView.width
                }

        animateWidgetPosition(params.x, targetX, 200)

        if (!isCollapsed) resetIdleTimer()
    }

    // Extracted helper — updates isDockedLeft based on current position
    private fun updateDockedSide() {
        val viewWidth = floatingView.width.takeIf { it > 0 } ?: currentPeekWidthPx
        val widgetCenter = params.x + (viewWidth / 2)
        isDockedLeft = widgetCenter < (screenWidth / 2)
    }

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
        if (!isCollapsed) floatingView.alpha = 1.0f
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, idleTimeout)
    }

    private fun updateDisplayMetrics() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        screenWidth = wm.currentWindowMetrics.bounds.width()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        floatingView.postOnAnimation {
            updateDisplayMetrics()
            val screenHeight = resources.displayMetrics.heightPixels
            params.y = params.y.coerceIn(0, screenHeight - floatingView.height)
            windowManager.updateViewLayout(floatingView, params)
            snapToEdge()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppPreferences.unregisterListener(this, prefListener)
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(deepDimRunnable)
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

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
}
