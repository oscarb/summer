package se.oscarb.summer.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import se.oscarb.summer.R

@OptIn(ExperimentalTextApi::class)
val NewsreaderFontFamily = FontFamily(
  Font(
    R.font.newsreader_regular,
    FontWeight.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(400))
  ),
  Font(
    R.font.newsreader_regular,
    FontWeight.Medium,
    variationSettings = FontVariation.Settings(FontVariation.weight(500))
  ),
  Font(
    R.font.newsreader_regular,
    FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600))
  ),
  Font(
    R.font.newsreader_regular,
    FontWeight.Bold,
    variationSettings = FontVariation.Settings(FontVariation.weight(700))
  )
)

@OptIn(ExperimentalTextApi::class)
val ManropeFontFamily = FontFamily(
  Font(
    R.font.manrope_regular,
    FontWeight.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(400))
  ),
  Font(
    R.font.manrope_regular,
    FontWeight.Medium,
    variationSettings = FontVariation.Settings(FontVariation.weight(500))
  ),
  Font(
    R.font.manrope_regular,
    FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600))
  ),
  Font(
    R.font.manrope_regular,
    FontWeight.Bold,
    variationSettings = FontVariation.Settings(FontVariation.weight(700))
  )
)

val Typography = Typography(
  displayLarge = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 56.sp,
    lineHeight = 64.sp,
    letterSpacing = (-0.02).sp
  ),
  displayMedium = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 40.sp,
    lineHeight = 48.sp,
    letterSpacing = (-0.01).sp
  ),
  displaySmall = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 32.sp,
    lineHeight = 40.sp
  ),
  headlineLarge = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.01).sp
  ),
  headlineMedium = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.01).sp
  ),
  headlineSmall = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 20.sp,
    lineHeight = 28.sp
  ),
  titleLarge = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp
  ),
  titleMedium = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 17.sp,
    lineHeight = 24.sp
  ),
  titleSmall = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 20.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = ManropeFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = ManropeFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
  ),
  bodySmall = TextStyle(
    fontFamily = ManropeFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.01.sp
  ),
  labelLarge = TextStyle(
    fontFamily = ManropeFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.02.sp
  ),
  labelMedium = TextStyle(
    fontFamily = ManropeFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.04.sp
  ),
  labelSmall = TextStyle(
    fontFamily = ManropeFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.06.sp
  )
)
