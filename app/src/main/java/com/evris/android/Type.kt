package com.evris.android

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val RobotoFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold)
)

val AppTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = RobotoFamily),
        displayMedium = displayMedium.copy(fontFamily = RobotoFamily),
        displaySmall = displaySmall.copy(fontFamily = RobotoFamily),
        headlineLarge = headlineLarge.copy(fontFamily = RobotoFamily),
        headlineMedium = headlineMedium.copy(fontFamily = RobotoFamily),
        headlineSmall = headlineSmall.copy(fontFamily = RobotoFamily),
        titleLarge = titleLarge.copy(fontFamily = RobotoFamily),
        titleMedium = titleMedium.copy(fontFamily = RobotoFamily),
        titleSmall = titleSmall.copy(fontFamily = RobotoFamily),
        bodyLarge = bodyLarge.copy(fontFamily = RobotoFamily),
        bodyMedium = bodyMedium.copy(fontFamily = RobotoFamily),
        bodySmall = bodySmall.copy(fontFamily = RobotoFamily),
        labelLarge = labelLarge.copy(fontFamily = RobotoFamily),
        labelMedium = labelMedium.copy(fontFamily = RobotoFamily),
        labelSmall = labelSmall.copy(fontFamily = RobotoFamily)
    )
}
