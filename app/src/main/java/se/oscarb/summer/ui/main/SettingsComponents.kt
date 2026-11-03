package se.oscarb.summer.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.oscarb.summer.theme.ManropeFontFamily
import se.oscarb.summer.theme.NewsreaderFontFamily
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerBorderSubtle
import se.oscarb.summer.theme.SummerError
import se.oscarb.summer.theme.SummerOnPrimaryContainer
import se.oscarb.summer.theme.SummerOnSurface
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerOutline
import se.oscarb.summer.theme.SummerPrimary
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSecondary
import se.oscarb.summer.theme.SummerSurfaceContainer
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import se.oscarb.summer.theme.SummerSurfaceContainerHighest
import se.oscarb.summer.theme.SummerSurfaceDim
import se.oscarb.summer.theme.SummerTextSecondary
import se.oscarb.summer.theme.SummerTheme
import se.oscarb.summer.util.ExternalBrowserOption

@Composable
internal fun SettingsSectionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color = SummerPrimaryContainer,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  Surface(
    color = SummerSurfaceContainer,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, SummerBorderSubtle),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = NewsreaderFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
          ),
          color = SummerPrimary,
          modifier = Modifier.weight(1f)
        )
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = ManropeFontFamily,
          fontSize = 13.sp,
          lineHeight = 18.sp
        ),
        color = SummerOnSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, start = 28.dp)
      )
      Spacer(modifier = Modifier.height(18.dp))
      content()
    }
  }
}

@Composable
internal fun SettingsSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontFamily = ManropeFontFamily,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold
        ),
        color = SummerOnSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = ManropeFontFamily,
          fontSize = 13.sp,
          lineHeight = 18.sp
        ),
        color = SummerOnSurfaceVariant,
        modifier = Modifier.padding(top = 3.dp)
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = SummerOnPrimaryContainer,
        checkedTrackColor = SummerPrimaryContainer,
        uncheckedThumbColor = SummerOnSurfaceVariant,
        uncheckedTrackColor = SummerSurfaceContainerHigh,
        uncheckedBorderColor = Color.Transparent
      )
    )
  }
}

@Composable
internal fun SettingsRadioCard(
  selected: Boolean,
  onClick: () -> Unit,
  title: String,
  subtitle: String,
  badgeText: String? = null,
  codeTag: String? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    color = if (selected) SummerPrimaryContainer.copy(alpha = 0.08f) else SummerSurfaceDim,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) SummerPrimaryContainer else SummerBorderSubtle
    ),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .padding(top = 2.dp)
          .size(18.dp)
          .clip(CircleShape)
          .border(
            width = if (selected) 2.dp else 1.5.dp,
            color = if (selected) SummerPrimaryContainer else SummerOutline.copy(alpha = 0.6f),
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        if (selected) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(SummerPrimaryContainer)
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 14.sp,
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (selected) SummerPrimary else SummerOnSurface
          )
          if (!badgeText.isNullOrBlank()) {
            Surface(
              color = if (selected) SummerPrimaryContainer.copy(alpha = 0.2f) else SummerSurfaceContainerHighest,
              shape = CircleShape
            ) {
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = ManropeFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.04.sp
                ),
                color = if (selected) SummerPrimary else SummerOnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = ManropeFontFamily,
            fontSize = 12.sp,
            lineHeight = 17.sp
          ),
          color = SummerOnSurfaceVariant,
          modifier = Modifier.padding(top = 3.dp)
        )
        if (!codeTag.isNullOrBlank()) {
          Surface(
            color = SummerSurfaceDim.copy(alpha = 0.9f),
            shape = CircleShape,
            border = BorderStroke(1.dp, SummerBorderSubtle),
            modifier = Modifier.padding(top = 6.dp)
          ) {
            Text(
              text = codeTag,
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              ),
              color = SummerSecondary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun SettingsDropdownSelector(
  label: String,
  selectedValue: String,
  selectedDisplay: String,
  options: List<Pair<String, String>>,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    Surface(
      onClick = { expanded = true },
      color = SummerSurfaceDim,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, SummerBorderSubtle),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 11.sp,
              color = SummerOnSurfaceVariant
            )
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = selectedDisplay,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = SummerOnSurface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = "Select $label",
          tint = SummerOutline,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier
        .heightIn(max = 340.dp)
        .background(SummerSurfaceContainerHigh)
        .border(1.dp, SummerBorderSubtle, RoundedCornerShape(12.dp))
    ) {
      options.forEach { (value, display) ->
        val isChosen = value == selectedValue
        DropdownMenuItem(
          text = {
            Text(
              text = display,
              color = if (isChosen) SummerPrimary else SummerOnSurface,
              fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
              fontSize = 13.sp,
              fontFamily = ManropeFontFamily
            )
          },
          onClick = {
            onSelect(value)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
internal fun VerificationStatusBox(
  message: String,
  isError: Boolean,
  modifier: Modifier = Modifier
) {
  Surface(
    color = if (isError) SummerError.copy(alpha = 0.12f) else SummerPrimaryContainer.copy(alpha = 0.12f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(
      width = 1.dp,
      color = if (isError) SummerError.copy(alpha = 0.35f) else SummerPrimaryContainer.copy(alpha = 0.35f)
    ),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = if (isError) Icons.Default.Close else Icons.Default.CheckCircle,
        contentDescription = null,
        tint = if (isError) SummerError else SummerPrimary,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = ManropeFontFamily,
          fontSize = 12.sp
        ),
        color = if (isError) SummerError else SummerPrimary
      )
    }
  }
}

@Preview(name = "Settings Browser Capture Section", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun SettingsBrowserCapturePreview() {
  SummerTheme {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      color = SummerBackground
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsSectionCard(
          title = "Google Feed & Browser Capture",
          subtitle = "Manage link interception and external reading tabs",
          icon = Icons.Default.Public
        ) {
          SettingsSwitchRow(
            title = "Only Intercept Google Feed URLs",
            subtitle = "Ignores routine searches, intercepts long-form articles",
            checked = true,
            onCheckedChange = {}
          )
          HorizontalDivider(
            color = SummerBorderSubtle.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 12.dp)
          )
          SettingsSwitchRow(
            title = "Use Custom Tabs overlay",
            subtitle = "When disabled, links open directly in your chosen browser",
            checked = false,
            onCheckedChange = {}
          )
          HorizontalDivider(
            color = SummerBorderSubtle.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 12.dp)
          )
          SettingsSwitchRow(
            title = "Don't save when opened from notification",
            subtitle = "Skips automatic queueing when tapping preview cards",
            checked = true,
            onCheckedChange = {}
          )
          HorizontalDivider(
            color = SummerBorderSubtle.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 14.dp)
          )
          Text(
            text = "TARGET EXTERNAL BROWSER",
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              letterSpacing = 0.06.sp
            ),
            color = SummerOnSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          val mockOptions = listOf(
            ExternalBrowserOption(packageName = "", label = "System Default"),
            ExternalBrowserOption(packageName = "com.android.chrome", label = "Chrome"),
            ExternalBrowserOption(packageName = "org.mozilla.firefox", label = "Firefox"),
            ExternalBrowserOption(packageName = "com.brave.browser", label = "Brave")
          )
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            mockOptions.chunked(2).forEach { rowOptions ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                rowOptions.forEach { option ->
                  val isSelected = option.packageName.isEmpty()
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SummerPrimaryContainer.copy(alpha = 0.12f) else SummerSurfaceDim,
                    border = BorderStroke(
                      width = if (isSelected) 1.5.dp else 1.dp,
                      color = if (isSelected) SummerPrimaryContainer else SummerBorderSubtle
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(46.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center
                    ) {
                      Icon(
                        imageVector = when {
                          option.packageName.isEmpty() -> Icons.Outlined.OpenInBrowser
                          option.label.contains("Chrome", ignoreCase = true) -> Icons.Outlined.Public
                          option.label.contains("Firefox", ignoreCase = true) -> Icons.Outlined.LocalFireDepartment
                          option.label.contains("Brave", ignoreCase = true) -> Icons.Outlined.Shield
                          else -> Icons.Outlined.Language
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) SummerPrimary else SummerOnSurfaceVariant
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                          fontFamily = ManropeFontFamily,
                          fontSize = 13.sp,
                          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isSelected) SummerPrimary else SummerOnSurface
                      )
                    }
                  }
                }
              }
            }
          }
        }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Summer v2.4.1 (Build 412) · Engine: gemini-3.8-flash-tts",
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              letterSpacing = 0.6.sp
            ),
            color = SummerTextSecondary.copy(alpha = 0.6f)
          )
        }
      }
    }
  }
}
