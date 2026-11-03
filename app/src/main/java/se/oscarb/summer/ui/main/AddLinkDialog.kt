package se.oscarb.summer.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import se.oscarb.summer.theme.ManropeFontFamily
import se.oscarb.summer.theme.NewsreaderFontFamily
import se.oscarb.summer.theme.SummerBorderSubtle
import se.oscarb.summer.theme.SummerOnPrimaryContainer
import se.oscarb.summer.theme.SummerOnSurface
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerOutline
import se.oscarb.summer.theme.SummerPrimary
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSurface
import se.oscarb.summer.theme.SummerSurfaceContainer
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun AddLinkDialog(
  onDismiss: () -> Unit,
  onAddUrl: (String) -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    AddLinkDialogContent(
      onDismiss = onDismiss,
      onAddUrl = onAddUrl
    )
  }
}

@Composable
internal fun AddLinkDialogContent(
  onDismiss: () -> Unit,
  onAddUrl: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var urlInput by rememberSaveable { mutableStateOf("") }
  val clipboardManager = LocalClipboardManager.current

  Surface(
    color = SummerSurfaceContainer,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, SummerBorderSubtle),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 28.dp)
      .wrapContentHeight()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          start = 24.dp,
          end = 24.dp,
          top = 28.dp,
          bottom = 22.dp
        )
    ) {
      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.AddLink,
          contentDescription = null,
          tint = SummerPrimary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Add link",
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = NewsreaderFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
          ),
          color = SummerOnSurface,
          modifier = Modifier.weight(1f)
        )
        IconButton(
          onClick = {
            val clipText = clipboardManager.getText()?.text?.trim()
            if (!clipText.isNullOrBlank()) {
              urlInput = clipText
            }
          },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ContentPaste,
            contentDescription = "Paste from clipboard",
            tint = SummerPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Input Field
      OutlinedTextField(
        value = urlInput,
        onValueChange = { urlInput = it },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = SummerOutline,
            modifier = Modifier.size(18.dp)
          )
        },
        trailingIcon = {
          if (urlInput.isNotBlank()) {
            IconButton(onClick = { urlInput = "" }, modifier = Modifier.size(24.dp)) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = SummerOutline,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = SummerSurfaceContainerHigh.copy(alpha = 0.8f),
          unfocusedContainerColor = SummerSurfaceContainerHigh.copy(alpha = 0.8f),
          focusedBorderColor = SummerPrimaryContainer,
          unfocusedBorderColor = SummerBorderSubtle,
          focusedTextColor = SummerOnSurface,
          unfocusedTextColor = SummerOnSurface,
          cursorColor = SummerPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = "Cancel",
            color = SummerOnSurfaceVariant,
            fontFamily = ManropeFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = {
            val trimmed = urlInput.trim()
            if (trimmed.isNotBlank()) {
              onAddUrl(trimmed)
            }
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SummerPrimaryContainer,
            contentColor = SummerOnPrimaryContainer
          )
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = SummerSurface
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Add link",
            color = SummerSurface,
            fontFamily = ManropeFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Preview(name = "Add Link Dialog", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun AddLinkDialogPreview() {
  SummerTheme {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      AddLinkDialogContent(
        onDismiss = {},
        onAddUrl = {}
      )
    }
  }
}
