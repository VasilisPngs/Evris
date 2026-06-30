package com.evris.android.play

import android.content.Context
import com.aurora.gplayapi.data.models.PlayFile
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object PlayDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun download(context: Context, files: List<PlayFile>): List<File> = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "play").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }

        files.map { file ->
            val name = file.name.ifBlank { "${UUID.randomUUID()}.apk" }
            val target = File(dir, name)
            val request = Request.Builder().url(file.url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Download failed HTTP ${response.code}")
                response.body.byteStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            target
        }
    }
}
