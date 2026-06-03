package com.example.data

import android.content.Context
import com.example.BuildConfig

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key_v2"
        private const val KEY_CUSTOM_INSTRUCTIONS = "custom_ai_instructions"
        private const val KEY_CUSTOM_CATEGORIES = "custom_ai_categories"
        private const val KEY_THEME = "theme_mode" // "light", "dark", or "system"
        private const val KEY_PIN_LOCKED = "pin_locked"
    }

    fun getGeminiApiKey(): String {
        val saved = prefs.getString(KEY_GEMINI_API_KEY, "")
        if (!saved.isNullOrEmpty()) {
            return saved
        }
        // Fallback to BuildConfig if defined and not default placeholder
        val buildKey = BuildConfig.GEMINI_API_KEY
        return if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
            buildKey
        } else {
            ""
        }
    }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun getCustomInstructions(): String {
        return prefs.getString(KEY_CUSTOM_INSTRUCTIONS, "") ?: ""
    }

    fun saveCustomInstructions(instructions: String) {
        prefs.edit().putString(KEY_CUSTOM_INSTRUCTIONS, instructions.trim()).apply()
    }

    fun getCustomCategories(): String {
        val defaultCategories = "سوبر ماركت, عيادة, راتب, طعام, مواصلات, ترفيه, تعليم, صحة, فواتير, بقالة"
        return prefs.getString(KEY_CUSTOM_CATEGORIES, defaultCategories) ?: defaultCategories
    }

    fun saveCustomCategories(categories: String) {
        prefs.edit().putString(KEY_CUSTOM_CATEGORIES, categories.trim()).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME, "light") ?: "light" // "light" or "dark" as user requested 2 themes
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
    }
}
