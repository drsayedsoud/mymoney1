package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = BrandTealDark,
    secondary = BrandTealSecDark,
    background = FinanceBgDark,
    surface = FinanceSurfaceDark,
    onPrimary = TextDarkGrid,
    onBackground = TextLightGrid,
    onSurface = TextLightGrid
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandTealLight,
    secondary = BrandTealSecLight,
    background = FinanceBgLight,
    surface = FinanceSurfaceLight,
    onPrimary = TextLightGrid,
    onBackground = TextDarkGrid,
    onSurface = TextDarkGrid
  )

@Composable
fun MyApplicationTheme(
  themeMode: String = "light",
  content: @Composable () -> Unit,
) {
  val darkTheme = themeMode == "dark"
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
