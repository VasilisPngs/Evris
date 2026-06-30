package com.evris.android.play

import android.content.Context
import android.util.Log
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.evris.android.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PlayRepository {
    @Volatile
    private var client: PlayHttpClient? = null

    private fun client(context: Context): PlayHttpClient {
        return client ?: synchronized(this) {
            client ?: PlayHttpClient(context.applicationContext).also { client = it }
        }
    }

    suspend fun checkUpdates(
        context: Context,
        installed: List<InstalledApp>
    ): Map<String, PlayUpdate> = withContext(Dispatchers.IO) {
        runCatching {
            val playClient = client(context)
            val auth = PlayAuth.get(context, playClient) ?: return@runCatching emptyMap<String, PlayUpdate>()
            val installedByPackage = installed.associateBy { it.packageName }
            val names = installed.map { it.packageName }

            AppDetailsHelper(auth)
                .using(playClient)
                .getAppByPackageName(names)
                .filter { app ->
                    app.versionCode > (installedByPackage[app.packageName]?.versionCode ?: Long.MAX_VALUE)
                }
                .associate { app ->
                    app.packageName to PlayUpdate(
                        packageName = app.packageName,
                        displayName = app.displayName,
                        versionName = app.versionName,
                        versionCode = app.versionCode,
                        offerType = app.offerType
                    )
                }
        }.onFailure { error ->
            Log.e("EvrisPlayRepo", "Update check failed", error)
        }.getOrDefault(emptyMap())
    }

    suspend fun files(
        context: Context,
        update: PlayUpdate
    ): List<PlayFile> = withContext(Dispatchers.IO) {
        runCatching {
            val playClient = client(context)
            val auth = PlayAuth.get(context, playClient) ?: return@runCatching emptyList<PlayFile>()

            PurchaseHelper(auth)
                .using(playClient)
                .purchase(update.packageName, update.versionCode, update.offerType)
                .filter { it.type == PlayFile.Type.BASE || it.type == PlayFile.Type.SPLIT }
        }.onFailure { error ->
            Log.e("EvrisPlayRepo", "File request failed for ${update.packageName}", error)
        }.getOrDefault(emptyList())
    }
}
