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
        const val TIMER_SECONDS = "timer_seconds"
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

    // ----- Timer seconds (0 = off) -----
    fun setTimerSeconds(seconds: Int) {
        prefs.edit().putInt(Keys.TIMER_SECONDS, seconds.coerceIn(0, 600)).apply()
    }

    fun getTimerSeconds(): Int = prefs.getInt(Keys.TIMER_SECONDS, 0)

    fun isTimerEnabled(): Boolean = getTimerSeconds() > 0

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

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}