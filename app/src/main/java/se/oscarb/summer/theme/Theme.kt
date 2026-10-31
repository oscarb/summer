package se.oscarb.summer.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SummerDarkColorScheme = darkColorScheme(
  primary = SummerPrimary,
  onPrimary = SummerOnPrimary,
  primaryContainer = SummerPrimaryContainer,
  onPrimaryContainer = SummerOnPrimaryContainer,
  secondary = SummerSecondary,
  onSecondary = SummerOnSecondary,
  secondaryContainer = SummerSecondaryContainer,
  onSecondaryContainer = SummerOnSecondaryContainer,
  tertiary = SummerTertiary,
  onTertiary = SummerOnTertiary,
  tertiaryContainer = SummerTertiaryContainer,
  onTertiaryContainer = SummerOnTertiaryContainer,
  background = SummerBackground,
  onBackground = SummerOnBackground,
  surface = SummerSurface,
  onSurface = SummerOnSurface,
  surfaceVariant = SummerSurfaceContainerHighest,
  onSurfaceVariant = SummerOnSurfaceVariant,
  surfaceContainer = SummerSurfaceContainer,
  surfaceContainerLow = SummerSurfaceContainerLow,
  surfaceContainerHigh = SummerSurfaceContainerHigh,
  surfaceContainerHighest = SummerSurfaceContainerHighest,
  surfaceContainerLowest = SummerSurfaceContainerLowest,
  surfaceBright = SummerSurfaceBright,
  surfaceDim = SummerSurfaceDim,
  outline = SummerOutline,
  outlineVariant = SummerOutlineVariant,
  error = SummerError,
  onError = SummerOnError,
  errorContainer = SummerErrorContainer,
  onErrorContainer = SummerOnErrorContainer
)

@Composable
fun SummerTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = SummerDarkColorScheme,
    typography = Typography,
    content = content
  )
}
