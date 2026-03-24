package com.example.volumeslider

import android.content.Context
import android.content.SharedPreferences

/**
 * Single source of truth for all user settings.
 *
 * Why a dedicated class instead of calling SharedPreferences directly?
 * If you ever need to change a key name, add validation, or migrate
 * old values, you do it in ONE place — not scattered across 3 files.
 *
 * 'object' in Kotlin = a singleton. There is exactly one instance
 * for the entire app lifetime, which is what we want for a prefs store.
 */
object AppPreferences {

    private const val PREF_FILE = "volume_slider_prefs"

    // Keys
    const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    const val KEY_WIDGET_SIZE     = "widget_size_dp"
    const val KEY_PEEK_WIDTH      = "peek_width_dp"

    // Defaults — these match what the service currently uses
    const val DEFAULT_OVERLAY_ENABLED = false
    const val DEFAULT_WIDGET_SIZE     = 56   // dp, matches our bug-fix button size
    const val DEFAULT_PEEK_WIDTH      = 32   // dp, matches our bug-fix peek width

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    // --- Readers ---

    fun isOverlayEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OVERLAY_ENABLED, DEFAULT_OVERLAY_ENABLED)

    fun getWidgetSizeDp(context: Context): Int =
        prefs(context).getInt(KEY_WIDGET_SIZE, DEFAULT_WIDGET_SIZE)

    fun getPeekWidthDp(context: Context): Int =
        prefs(context).getInt(KEY_PEEK_WIDTH, DEFAULT_PEEK_WIDTH)

    // --- Writers ---

    fun setOverlayEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()

    fun setWidgetSizeDp(context: Context, sizeDp: Int) =
        prefs(context).edit().putInt(KEY_WIDGET_SIZE, sizeDp).apply()

    fun setPeekWidthDp(context: Context, peekDp: Int) =
        prefs(context).edit().putInt(KEY_PEEK_WIDTH, peekDp).apply()

    // --- Listener helper ---

    /**
     * Register a listener that fires whenever ANY preference changes.
     * The caller is responsible for unregistering this to avoid memory leaks.
     * Usage: val listener = AppPreferences.registerListener(context) { key -> ... }
     *        AppPreferences.unregisterListener(context, listener)
     */
    fun registerListener(
        context: Context,
        onChange: (key: String?) -> Unit
    ): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            onChange(key)
        }
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun unregisterListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}