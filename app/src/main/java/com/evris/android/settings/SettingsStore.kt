package com.evris.android.settings

import android.content.Context

object SettingsStore {
    private const val NAME = "evris_settings"
    private const val INCLUDE_DEV = "include_dev"
    private const val INCLUDE_ALPHA = "include_alpha"
    private const val INCLUDE_BETA = "include_beta"
    private const val INCLUDE_RC = "include_rc"
    private const val INCLUDE_PRERELEASE = "include_prerelease"
    private const val THEME_MODE = "theme_mode"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    private const val PLAY_ENABLED = "play_enabled"
    private const val PLAY_AUTH = "play_auth"
    private const val LAST_PLAY_CHECK = "last_play_check"

    fun read(context: Context): ReleaseChannelSettings {
        val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

        return ReleaseChannelSettings(
            includeDev = prefs.getBoolean(INCLUDE_DEV, false),
            includeAlpha = prefs.getBoolean(INCLUDE_ALPHA, false),
            includeBeta = prefs.getBoolean(INCLUDE_BETA, false),
            includeRc = prefs.getBoolean(INCLUDE_RC, false),
            includePrerelease = prefs.getBoolean(INCLUDE_PRERELEASE, false)
        )
    }

    fun write(
        context: Context,
        settings: ReleaseChannelSettings
    ) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(INCLUDE_DEV, settings.includeDev)
            .putBoolean(INCLUDE_ALPHA, settings.includeAlpha)
            .putBoolean(INCLUDE_BETA, settings.includeBeta)
            .putBoolean(INCLUDE_RC, settings.includeRc)
            .putBoolean(INCLUDE_PRERELEASE, settings.includePrerelease)
            .apply()
    }

    fun readThemeMode(context: Context): Int {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getInt(THEME_MODE, THEME_SYSTEM)
    }

    fun readPlayEnabled(context: Context): Boolean {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(PLAY_ENABLED, false)
    }

    fun writePlayEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PLAY_ENABLED, enabled)
            .apply()
    }

    fun readPlayAuth(context: Context): String {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(PLAY_AUTH, "") ?: ""
    }

    fun writePlayAuth(context: Context, value: String) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PLAY_AUTH, value)
            .apply()
    }

    fun readLastPlayCheck(context: Context): Long {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getLong(LAST_PLAY_CHECK, 0L)
    }

    fun writeLastPlayCheck(context: Context, value: Long) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_PLAY_CHECK, value)
            .apply()
    }

    fun writeThemeMode(
        context: Context,
        mode: Int
    ) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(THEME_MODE, mode)
            .apply()
    }
}
