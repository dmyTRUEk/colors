package dmytruek.colors

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SettingsManager {
    private const val PREFS_NAME = "colors_settings"
    private const val KEY_MIN_POS = "reinsert_min_pos"
    private const val KEY_MAX_POS = "reinsert_max_pos"
    private const val KEY_CANDIDATES_COUNT = "candidates_count"
    private const val KEY_DISABLED_COLORS = "disabled_colors"

    const val DEFAULT_MIN_POS = 5
    const val DEFAULT_MAX_POS = 15
    const val DEFAULT_CANDIDATES_COUNT = 10

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getMinPos(context: Context): Int {
        return getPrefs(context).getInt(KEY_MIN_POS, DEFAULT_MIN_POS)
    }

    fun getMaxPos(context: Context): Int {
        return getPrefs(context).getInt(KEY_MAX_POS, DEFAULT_MAX_POS)
    }

    fun getCandidatesCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_CANDIDATES_COUNT, DEFAULT_CANDIDATES_COUNT)
    }

    fun getDisabledColors(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_DISABLED_COLORS, emptySet()) ?: emptySet()
    }

    fun setDisabledColors(context: Context, disabled: Set<String>) {
        getPrefs(context).edit {
            putStringSet(KEY_DISABLED_COLORS, disabled)
        }
    }

    fun getActiveColors(context: Context): List<ColorData> {
        val disabled = getDisabledColors(context)
        val active = allColors.filter { it.name !in disabled }
        return if (active.size >= 4) active else allColors
    }

    fun saveSettings(context: Context, min: Int, max: Int, candidatesCount: Int) {
        getPrefs(context).edit {
            putInt(KEY_MIN_POS, min)
            putInt(KEY_MAX_POS, max)
            putInt(KEY_CANDIDATES_COUNT, candidatesCount)
        }
    }
}
