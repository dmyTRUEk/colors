package dmytruek.colors

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SettingsManager {
    private const val PREFS_NAME = "colors_settings"
    private const val KEY_MIN_POS = "reinsert_min_pos"
    private const val KEY_MAX_POS = "reinsert_max_pos"

    const val DEFAULT_MIN_POS = 5
    const val DEFAULT_MAX_POS = 15

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getMinPos(context: Context): Int {
        return getPrefs(context).getInt(KEY_MIN_POS, DEFAULT_MIN_POS)
    }

    fun getMaxPos(context: Context): Int {
        return getPrefs(context).getInt(KEY_MAX_POS, DEFAULT_MAX_POS)
    }

    fun savePositions(context: Context, min: Int, max: Int) {
        getPrefs(context).edit {
            putInt(KEY_MIN_POS, min)
            putInt(KEY_MAX_POS, max)
        }
    }
}
