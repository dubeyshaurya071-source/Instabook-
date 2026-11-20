package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val InstabookLightColorScheme = lightColorScheme(
    primary = InstabookBlue,
    onPrimary = Color.White,
    primaryContainer = InstabookIceBlue,
    onPrimaryContainer = InstabookDeepBlue,
    secondary = InstabookDeepBlue,
    onSecondary = Color.White,
    secondaryContainer = InstabookSoftBlue,
    onSecondaryContainer = InstabookDeepBlue,
    tertiary = InstabookSkyBlue,
    onTertiary = InstabookInk,
    background = InstabookSurfaceWhite,
    onBackground = InstabookInk,
    surface = InstabookSurfaceWhite,
    onSurface = InstabookInk,
    surfaceVariant = InstabookSurfaceTint,
    onSurfaceVariant = InstabookSlate,
    outline = InstabookSoftBlue,
    outlineVariant = Color(0xFFE2E8F0),
    error = InstabookError,
    onError = Color.White
)

private val InstabookShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun InstabookTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = InstabookLightColorScheme,
        typography = Typography,
        shapes = InstabookShapes,
        content = content
    )
}
