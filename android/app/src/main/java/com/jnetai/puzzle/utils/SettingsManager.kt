package com.jnetai.puzzle.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Centralised settings storage. All values survive app restarts.
 */
class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    object Keys {
        const val GRID_SIZE = "grid_size"
        const val TIMER_MODE = "timer_mode"
        const val TIMER_LIMIT_SECONDS = "timer_limit_seconds"
        const val FAVOURITES = "favourites"
        const val HIDDEN = "hidden_images"
    }

    // ----- Grid size (3, 4, 5 or 6) -----
    fun setGridSize(size: Int) {
        prefs.edit().putInt(Keys.GRID_SIZE, size.coerceIn(3, 6)).apply()
    }

    fun getGridSize(): Int {
        val size = prefs.getInt(Keys.GRID_SIZE, 4)
        return if (size in 3..6) size else {
            ErrorLogger.logf(ErrorLogger.Codes.SET_INVALID_VALUE,
                "Unknown stored grid size '%d', resetting to 4", size)
            4
        }
    }

    // ----- Timer -----
    // mode: "off" = no timer, "up" = counts up (just timing the game/puzzle),
    //       "down" = counts down from a limit (the game ends when time runs out).
    fun setTimerMode(mode: String) {
        val safe = when (mode) {
            TimerMode.UP, TimerMode.DOWN -> mode
            else -> TimerMode.OFF
        }
        prefs.edit().putString(Keys.TIMER_MODE, safe).apply()
    }

    fun getTimerMode(): String {
        val mode = prefs.getString(Keys.TIMER_MODE, TimerMode.OFF) ?: TimerMode.OFF
        return when (mode) {
            TimerMode.UP, TimerMode.DOWN -> mode
            else -> TimerMode.OFF
        }
    }

    fun setTimerLimitSeconds(seconds: Int) {
        prefs.edit().putInt(Keys.TIMER_LIMIT_SECONDS, seconds.coerceIn(10, 3600)).apply()
    }

    fun getTimerLimitSeconds(): Int {
        val seconds = prefs.getInt(Keys.TIMER_LIMIT_SECONDS, 120)
        return if (seconds in 10..3600) seconds else {
            ErrorLogger.logf(ErrorLogger.Codes.SET_INVALID_VALUE,
                "Unknown stored timer limit '%d', resetting to 120s", seconds)
            120
        }
    }

    fun isTimerEnabled(): Boolean = getTimerMode() != TimerMode.OFF

    // ----- Favourite images -----
    fun isFavourite(imageName: String): Boolean = getFavourites().contains(imageName)

    fun toggleFavourite(imageName: String): Boolean {
        val set = getFavourites().toMutableSet()
        val added = if (!set.add(imageName)) {
            set.remove(imageName)
            false
        } else true
        prefs.edit().putStringSet(Keys.FAVOURITES, set).apply()
        return added
    }

    fun getFavourites(): Set<String> =
        prefs.getStringSet(Keys.FAVOURITES, emptySet()) ?: emptySet()

    // ----- Hidden built-in images -----
    fun isHidden(imageName: String): Boolean = getHiddenImages().contains(imageName)

    fun setHidden(imageName: String, hidden: Boolean) {
        val set = getHiddenImages().toMutableSet()
        if (hidden) set.add(imageName) else set.remove(imageName)
        prefs.edit().putStringSet(Keys.HIDDEN, set).apply()
    }

    fun getHiddenImages(): Set<String> =
        prefs.getStringSet(Keys.HIDDEN, emptySet()) ?: emptySet()

    companion object {
        private const val PREFS_NAME = "puzzle_settings"

        object TimerMode {
            const val OFF = "off"
            const val UP = "up"
            const val DOWN = "down"
        }

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}