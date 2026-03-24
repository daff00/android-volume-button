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
    private val dragSlop = 8f

    // Set to false when collapsed so the peek tab gets
    // clean tap events without drag interception
    var isDragEnabled = true

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!isDragEnabled) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.rawX - startX)
                val dy = abs(event.rawY - startY)
                if (dx > dragSlop || dy > dragSlop) {
                    return true
                }
            }
        }
        return false
    }
}