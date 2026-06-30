package com.evris.android.play

data class PlayUpdate(
    val packageName: String,
    val displayName: String,
    val versionName: String,
    val versionCode: Long,
    val offerType: Int
)
