package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CameraColorScheme =
  darkColorScheme(
    primary = CameraTextWhite,
    onPrimary = CameraBlack,
    secondary = CameraPillActive,
    onSecondary = CameraTextWhite,
    background = CameraBlack,
    onBackground = CameraTextWhite,
    surface = CameraBlack,
    onSurface = CameraTextWhite,
    surfaceVariant = CameraPillBg,
    onSurfaceVariant = CameraTextWhite,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = CameraColorScheme, typography = Typography, content = content)
}
