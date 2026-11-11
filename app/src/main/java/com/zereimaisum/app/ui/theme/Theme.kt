package com.zereimaisum.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zereimaisum.app.data.session.ThemeMode

/** Identidade visual Zerei Mais Um */
object ZmColors {
    val Navy = Color(0xFF09295B)
    val Blue = Color(0xFF0879E8)
    val LightBlue = Color(0xFF43D5F2)
    val Green = Color(0xFF8DEB35)
    val White = Color(0xFFFFFFFF)

    val Income = Color(0xFF2E9E4F)
    val Expense = Color(0xFFD64545)
    val Warning = Color(0xFFF2A541)
}

private val Light = lightColorScheme(
    primary = ZmColors.Blue,
    onPrimary = ZmColors.White,
    primaryContainer = Color(0xFFD6E8FC),
    onPrimaryContainer = ZmColors.Navy,
    secondary = ZmColors.Navy,
    onSecondary = ZmColors.White,
    tertiary = Color(0xFF4E8F12),
    tertiaryContainer = Color(0xFFE3F9CC),
    onTertiaryContainer = Color(0xFF1E3A05),
    background = Color(0xFFF6F8FC),
    onBackground = ZmColors.Navy,
    surface = ZmColors.White,
    onSurface = Color(0xFF14213D),
    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = Color(0xFF4A5874),
    error = ZmColors.Expense,
)

private val Dark = darkColorScheme(
    primary = ZmColors.LightBlue,
    onPrimary = ZmColors.Navy,
    primaryContainer = Color(0xFF123E7A),
    onPrimaryContainer = Color(0xFFD6E8FC),
    secondary = ZmColors.Blue,
    onSecondary = ZmColors.White,
    tertiary = ZmColors.Green,
    tertiaryContainer = Color(0xFF2C4A10),
    onTertiaryContainer = Color(0xFFE3F9CC),
    background = Color(0xFF061A3A),
    onBackground = Color(0xFFE6EEFA),
    surface = Color(0xFF0B2550),
    onSurface = Color(0xFFE6EEFA),
    surfaceVariant = Color(0xFF14315F),
    onSurfaceVariant = Color(0xFFB5C4DE),
    error = Color(0xFFFF8A80),
)

private val ZmTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
)

private val ZmShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun ZmTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = ZmTypography,
        shapes = ZmShapes,
        content = content,
    )
}
