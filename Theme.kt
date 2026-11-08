package com.nebula.app.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NebulaColors {
    val Background = Color(0xFFF7F7F8)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF17171B)
    val Secondary = Color(0xFF777780)
    val Border = Color(0xFFE7E7EA)
    val Subtle = Color(0xFFEFEFF2)
    val Accent = Color(0xFF7655F5)
    val AccentPressed = Color(0xFF6241DD)
    val AccentSoft = Color(0xFFEFEAFE)
    val Error = Color(0xFFC0392B)
    val ErrorSoft = Color(0xFFFBECEA)
    val Success = Color(0xFF2E7D55)
    val SuccessSoft = Color(0xFFE8F4EE)
    val Warning = Color(0xFF9A6A12)
    val WarningSoft = Color(0xFFFAF1DD)
}

private val NebulaColorScheme = lightColorScheme(
    primary = NebulaColors.Accent,
    onPrimary = Color.White,
    primaryContainer = NebulaColors.AccentSoft,
    onPrimaryContainer = NebulaColors.AccentPressed,
    secondary = NebulaColors.Ink,
    onSecondary = Color.White,
    background = NebulaColors.Background,
    onBackground = NebulaColors.Ink,
    surface = NebulaColors.Surface,
    onSurface = NebulaColors.Ink,
    surfaceVariant = NebulaColors.Subtle,
    onSurfaceVariant = NebulaColors.Secondary,
    outline = NebulaColors.Border,
    outlineVariant = NebulaColors.Border,
    error = NebulaColors.Error,
    onError = Color.White
)

private val NebulaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val Sans = FontFamily.SansSerif

private val NebulaTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

/** Nebula is light mode only in this release. */
@Composable
fun NebulaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NebulaColorScheme,
        shapes = NebulaShapes,
        typography = NebulaTypography,
        content = content
    )
}
