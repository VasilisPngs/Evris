package com.evris.android.play

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object PlayInstaller {
    private const val ACTION = "com.evris.android.PLAY_INSTALL"

    fun canInstall(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun requestPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    @SuppressLint("MissingPermission")
    suspend fun install(context: Context, packageName: String, files: List<File>): Boolean {
        if (files.isEmpty()) return false

        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(packageName)
        if (Build.VERSION.SDK_INT >= 31) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        if (Build.VERSION.SDK_INT >= 33) params.setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)

        val action = "$ACTION.${System.currentTimeMillis()}"

        return suspendCancellableCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(received: Context, intent: Intent) {
                    when (intent.extras?.getInt(PackageInstaller.EXTRA_STATUS)) {
                        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                            val confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                            confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(confirm) }
                        }
                        PackageInstaller.STATUS_SUCCESS -> {
                            runCatching { context.unregisterReceiver(this) }
                            if (continuation.isActive) continuation.resume(true)
                        }
                        else -> {
                            runCatching { context.unregisterReceiver(this) }
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                }
            }

            val filter = IntentFilter(action)
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }

            continuation.invokeOnCancellation {
                runCatching { context.unregisterReceiver(receiver) }
            }

            runCatching {
                val sessionId = installer.createSession(params)
                installer.openSession(sessionId).use { session ->
                    files.forEach { file ->
                        file.inputStream().use { input ->
                            session.openWrite(file.name, 0, file.length()).use { output ->
                                input.copyTo(output)
                                session.fsync(output)
                            }
                        }
                    }
                    val intent = Intent(action).setPackage(context.packageName)
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
                    session.commit(pending.intentSender)
                }
                files.forEach { runCatching { it.delete() } }
            }.onFailure {
                runCatching { context.unregisterReceiver(receiver) }
                if (continuation.isActive) continuation.resume(false)
            }
        }
    }
}
