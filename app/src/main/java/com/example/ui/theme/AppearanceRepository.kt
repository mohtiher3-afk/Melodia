package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository for managing app appearance settings (theme mode, accent color).
 * Uses SharedPreferences backed by Flow for reactive UI updates.
 */
class AppearanceRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "melodia_appearance"
        private const val THEME_MODE_KEY = "theme_mode"
        private const val ACCENT_COLOR_KEY = "accent_color"
    }

    private val _appearance = MutableStateFlow(AppAppearance())
    val appearance: Flow<AppAppearance> = _appearance.asStateFlow()

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeModeName = prefs.getString(THEME_MODE_KEY, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val accentColorName = prefs.getString(ACCENT_COLOR_KEY, AccentColor.DEFAULT.name) ?: AccentColor.DEFAULT.name

        val themeMode = runCatching { ThemeMode.valueOf(themeModeName) }.getOrElse { ThemeMode.SYSTEM }
        val accentColor = runCatching { AccentColor.valueOf(accentColorName) }.getOrElse { AccentColor.DEFAULT }

        _appearance.value = AppAppearance(themeMode = themeMode, accentColor = accentColor)
    }

    fun setThemeMode(themeMode: ThemeMode) {
        _appearance.value = _appearance.value.copy(themeMode = themeMode)
        persistPreference(THEME_MODE_KEY, themeMode.name)
    }

    fun setAccentColor(accentColor: AccentColor) {
        _appearance.value = _appearance.value.copy(accentColor = accentColor)
        persistPreference(ACCENT_COLOR_KEY, accentColor.name)
    }

    private fun persistPreference(key: String, value: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
        }
    }
}