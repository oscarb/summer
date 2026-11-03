package se.oscarb.summer.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.oscarb.summer.theme.ManropeFontFamily
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerPrimary
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun FloatingCatchMeUpPill(
  unreadCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    color = SummerPrimaryContainer,
    contentColor = SummerBackground,
    shape = CircleShape,
    shadowElevation = 10.dp,
    border = BorderStroke(1.dp, SummerPrimary.copy(alpha = 0.5f)),
    modifier = modifier.height(48.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
      Icon(
        imageVector = Icons.Default.GraphicEq,
        contentDescription = null,
        tint = SummerBackground,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Catch Me Up",
        style = MaterialTheme.typography.labelMedium.copy(
          fontFamily = ManropeFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        ),
        color = SummerBackground
      )
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        shape = CircleShape,
        color = SummerBackground
      ) {
        Text(
          text = "$unreadCount",
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          ),
          color = SummerPrimaryContainer,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
      }
    }
  }
}

@Preview(name = "Catch Me Up Pill", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun CatchMeUpPillPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      FloatingCatchMeUpPill(
        unreadCount = 3,
        onClick = {}
      )
    }
  }
}
