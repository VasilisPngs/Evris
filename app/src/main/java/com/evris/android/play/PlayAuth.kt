package com.evris.android.play

import android.content.Context
import android.util.Log
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.evris.android.settings.SettingsStore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PlayAuth {
    private const val AUTH_URL = "https://auroraoss.com/api/auth"
    private const val VALIDITY_MS = 60 * 60 * 1000L
    private val gson = Gson()

    suspend fun get(context: Context, client: PlayHttpClient): AuthData? = withContext(Dispatchers.IO) {
        val saved = load(context)

        if (saved == null || saved.email.isEmpty()) {
            return@withContext refresh(context, client)
        }

        if (System.currentTimeMillis() - SettingsStore.readLastPlayCheck(context) > VALIDITY_MS) {
            SettingsStore.writeLastPlayCheck(context, System.currentTimeMillis())

            val valid = runCatching {
                val app = AppDetailsHelper(saved)
                    .using(client)
                    .getAppByPackageName("com.google.android.gm")

                app.packageName.isNotEmpty()
            }.getOrElse { error ->
                Log.e("EvrisPlayAuth", "Token validation failed", error)
                false
            }

            if (!valid) return@withContext refresh(context, client)
        }

        saved
    }

    private fun load(context: Context): AuthData? {
        val raw = SettingsStore.readPlayAuth(context)
        if (raw.isBlank()) return null

        return runCatching { gson.fromJson(raw, AuthData::class.java) }
            .onFailure { Log.e("EvrisPlayAuth", "Stored auth parsing failed", it) }
            .getOrNull()
    }

    private fun refresh(context: Context, client: PlayHttpClient): AuthData? {
        return runCatching {
            Log.i("EvrisPlayAuth", "Refreshing token")
            val payload = gson.toJson(nativeDeviceProperties(context)).toByteArray()
            val response = client.postAuth(AUTH_URL, payload)

            if (!response.isSuccessful) {
                Log.e("EvrisPlayAuth", "Auth failed ${response.code} ${response.errorString}")
                return@runCatching null
            }

            val text = String(response.responseBytes)
            val auth = gson.fromJson(text, AuthData::class.java)
            SettingsStore.writePlayAuth(context, text)
            SettingsStore.writeLastPlayCheck(context, System.currentTimeMillis())
            auth
        }.onFailure { error ->
            Log.e("EvrisPlayAuth", "Auth refresh failed", error)
        }.getOrNull()
    }
}
