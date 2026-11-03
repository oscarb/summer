package se.oscarb.summer.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import se.oscarb.summer.theme.ManropeFontFamily
import se.oscarb.summer.theme.NewsreaderFontFamily
import se.oscarb.summer.theme.SummerBorderSubtle
import se.oscarb.summer.theme.SummerCardBackground
import se.oscarb.summer.theme.SummerOutline
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import kotlin.math.hypot
import java.util.Locale

internal fun formatDurationMs(ms: Long): String {
  if (ms <= 0L) return "00:00"
  val totalSeconds = ms / 1000L
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

// ── Feed card design tokens (mirrors the Stitch "Summer" design system) ──────────────────────

/** Stitch `headline-sm` + `leading-snug`: Newsreader 20/27.5, medium. */
internal val CardHeadlineStyle = TextStyle(
  fontFamily = NewsreaderFontFamily,
  fontWeight = FontWeight.Medium,
  fontSize = 20.sp,
  lineHeight = 27.5.sp
)

/** Stitch `body-sm` + `leading-relaxed`: Manrope 13/21. */
internal val CardBodyStyle = TextStyle(
  fontFamily = ManropeFontFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 13.sp,
  lineHeight = 21.sp,
  letterSpacing = 0.01.em
)

/** Stitch `label-sm`: Manrope 11/14, bold, tracked 0.06em. */
internal val CardLabelStyle = TextStyle(
  fontFamily = ManropeFontFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 11.sp,
  lineHeight = 14.sp,
  letterSpacing = 0.06.em
)

/** Untracked 11sp Manrope for metadata (hosts, counts, timestamps). */
internal val CardMetaStyle = TextStyle(
  fontFamily = ManropeFontFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 11.sp,
  lineHeight = 14.sp
)

internal val CardDividerColor = SummerBorderSubtle.copy(alpha = 0.6f)
internal val WaveformIdleBarColor = Color(0xFF4F4539).copy(alpha = 0.6f)

/**
 * Feathers a thumbnail into [base] like the Stitch mock's stacked CSS gradients: a radial vignette
 * anchored top-right, plus linear fades towards the left and bottom edges.
 */
internal fun Modifier.thumbnailFeather(
  base: Color,
  leftFadeStart: Float,
  bottomFadeStart: Float
): Modifier = this.drawWithCache {
  val radial = Brush.radialGradient(
    0.20f to Color.Transparent,
    0.60f to base.copy(alpha = 0.4f),
    0.96f to base,
    center = Offset(size.width, 0f),
    radius = hypot(size.width, size.height).coerceAtLeast(1f)
  )
  val towardsLeft = Brush.horizontalGradient(
    0.05f to base,
    (1f - leftFadeStart) to Color.Transparent
  )
  val towardsBottom = Brush.verticalGradient(
    bottomFadeStart to Color.Transparent,
    0.98f to base
  )
  onDrawBehind {
    drawRect(radial)
    drawRect(towardsLeft)
    drawRect(towardsBottom)
  }
}

/**
 * Subtle warm glow bleeding in from the top-right corner (Stitch: blurred `primary-container/10`
 * disc), layered over a faint top-down sheen so the card reads slightly lit from above.
 */
internal fun Modifier.episodeCardGlow(intensity: Float): Modifier = this.drawWithCache {
  val glowCenter = Offset(size.width - 16.dp.toPx(), 16.dp.toPx())
  val glow = Brush.radialGradient(
    0f to SummerPrimaryContainer.copy(alpha = intensity),
    0.45f to SummerPrimaryContainer.copy(alpha = intensity * 0.45f),
    1f to Color.Transparent,
    center = glowCenter,
    radius = 140.dp.toPx()
  )
  val sheen = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.025f),
    0.55f to Color.Transparent
  )
  onDrawBehind {
    drawRect(sheen)
    drawRect(glow)
  }
}

@Composable
internal fun FeatheredThumbnail(
  url: String,
  leftFadeStart: Float,
  bottomFadeStart: Float,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier) {
    AsyncImage(
      model = url,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      placeholder = ColorPainter(SummerSurfaceContainerHigh),
      modifier = Modifier.fillMaxSize()
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .thumbnailFeather(SummerCardBackground, leftFadeStart, bottomFadeStart)
    )
  }
}

@Composable
internal fun CardToolbarIcon(
  imageVector: ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
    Icon(
      imageVector = imageVector,
      contentDescription = contentDescription,
      modifier = Modifier.size(18.dp),
      tint = SummerOutline
    )
  }
}
