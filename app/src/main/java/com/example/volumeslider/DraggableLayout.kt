package com.example.volumeslider

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import kotlin.math.abs

class DraggableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var startX = 0f
    private var startY = 0f
    // How many pixels of movement before we "claim" the drag.
    // 8px is deliberate — enough to ignore tiny finger trembles,
    // small enough to feel instant.
    private val dragSlop = 8f

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Record where the finger landed. Do NOT intercept yet —
                // let the child Button see this so it shows its pressed state.
                startX = event.rawX
                startY = event.rawY
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.rawX - startX)
                val dy = abs(event.rawY - startY)
                // The moment we detect real movement, intercept ALL future
                // events in this gesture. The Button never sees ACTION_UP,
                // so it won't fire a click. Clean drag takeover.
                if (dx > dragSlop || dy > dragSlop) {
                    return true
                }
            }
        }
        return false
    }
}