package se.oscarb.summer.ui.main

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import se.oscarb.summer.data.SavedWebsite
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerBorderSubtle
import se.oscarb.summer.theme.SummerCardBackground
import se.oscarb.summer.theme.SummerDomain
import se.oscarb.summer.theme.SummerError
import se.oscarb.summer.theme.SummerErrorContainer
import se.oscarb.summer.theme.SummerHeadline
import se.oscarb.summer.theme.SummerOnSurface
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerOutline
import se.oscarb.summer.theme.SummerPrimary
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.ui.components.MarkdownText

@Composable
internal fun WebsiteItemCard(
  website: SavedWebsite,
  isExpanded: Boolean,
  onCardClick: () -> Unit,
  onSummarizeClick: () -> Unit,
  onToggleArchive: () -> Unit,
  onDelete: () -> Unit,
  onOpenExternal: () -> Unit
) {
  val context = LocalContext.current
  val hasThumbnail = website.thumbnailUrl.isNotBlank()
  val title = website.title.ifBlank { website.domain }

  Surface(
    onClick = onCardClick,
    color = SummerCardBackground,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, SummerBorderSubtle),
    shadowElevation = if (isExpanded) 6.dp else 3.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    if (!isExpanded) {
      // Collapsed: 88dp min height, 96dp feathered thumbnail bleeding off the right edge.
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 88.dp)
          .height(IntrinsicSize.Min)
      ) {
        if (hasThumbnail) {
          FeatheredThumbnail(
            url = website.thumbnailUrl,
            leftFadeStart = 0.35f,
            bottomFadeStart = 0.35f,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .size(96.dp)
          )
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.CenterStart)
            .padding(
              start = 16.dp,
              end = if (hasThumbnail) 72.dp else 24.dp,
              top = 20.dp,
              bottom = 20.dp
            )
        ) {
          Text(
            text = title,
            style = CardHeadlineStyle,
            color = SummerHeadline,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(12.dp))
          WebsiteSourceRow(website = website, showAiBadge = website.summary != null)
        }
      }
    } else {
      // Expanded: 148dp feathered hero thumbnail top-right, title/domain beside it, full-width body.
      Box(modifier = Modifier.fillMaxWidth()) {
        if (hasThumbnail) {
          FeatheredThumbnail(
            url = website.thumbnailUrl,
            leftFadeStart = 0.35f,
            bottomFadeStart = 0.35f,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .size(96.dp)
          )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(
                start = 16.dp,
                top = 20.dp,
                end = if (hasThumbnail) 72.dp else 24.dp,
                bottom = if (hasThumbnail) 8.dp else 12.dp
              )
          ) {
            Text(
              text = title,
              style = CardHeadlineStyle,
              color = SummerHeadline,
              maxLines = 4,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            WebsiteSourceRow(website = website, showAiBadge = true)
          }

          Spacer(modifier = Modifier.height(12.dp))

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 16.dp, bottom = 13.dp)
          ) {
            when {
              website.isSummarizing -> SummarySkeletonLoader()
              website.summaryError != null -> {
                Surface(
                  color = SummerErrorContainer.copy(alpha = 0.4f),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                      text = website.summaryError,
                      style = CardBodyStyle,
                      color = SummerError
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onSummarizeClick) {
                      Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text("Retry Summary")
                    }
                  }
                }
              }
              website.summary != null -> {
                val cleanSummary = remember(website.summary) {
                  stripTlDr(website.summary)
                }
                MarkdownText(
                  markdown = cleanSummary,
                  style = CardBodyStyle,
                  color = SummerOnSurface,
                  listItemColor = SummerOnSurface,
                  blockSpacing = 10.dp,
                  listItemSpacing = 6.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = null,
                      modifier = Modifier.size(12.dp),
                      tint = SummerPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                      text = website.summaryProvider ?: "AI Summary",
                      style = CardMetaStyle.copy(fontWeight = FontWeight.Medium),
                      color = SummerOutline
                    )
                  }
                  IconButton(
                    onClick = onSummarizeClick,
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Refresh,
                      contentDescription = "Regenerate summary",
                      modifier = Modifier.size(14.dp),
                      tint = SummerOutline
                    )
                  }
                }
              }
              else -> {
                Button(
                  onClick = onSummarizeClick,
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = SummerPrimary,
                    contentColor = SummerBackground
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                ) {
                  Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(
                    text = "Summarize Article",
                    style = CardLabelStyle.copy(
                      fontSize = 12.sp,
                      lineHeight = 16.sp,
                      fontWeight = FontWeight.SemiBold,
                      letterSpacing = 0.04.em
                    )
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CardDividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(9.dp))

            // Bottom action toolbar
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(onClick = onOpenExternal)
                  .padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = SummerOnSurfaceVariant
                )
                Text(
                  text = "Open",
                  style = CardLabelStyle,
                  color = SummerOnSurfaceVariant
                )
              }

              Row(
                modifier = Modifier.offset(x = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                CardToolbarIcon(
                  imageVector = if (website.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                  contentDescription = if (website.isArchived) "Unarchive" else "Archive",
                  onClick = onToggleArchive
                )
                CardToolbarIcon(
                  imageVector = Icons.Default.Share,
                  contentDescription = "Share",
                  onClick = {
                    val shareText = buildString {
                      appendLine(website.title)
                      appendLine(website.url)
                      if (!website.summary.isNullOrBlank()) {
                        appendLine()
                        appendLine("Summary (via Summer):")
                        appendLine(website.summary)
                      }
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share website"))
                  }
                )
                CardToolbarIcon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete",
                  onClick = onDelete
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun WebsiteSourceRow(website: SavedWebsite, showAiBadge: Boolean) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    if (website.faviconUrl.isNotBlank()) {
      AsyncImage(
        model = website.faviconUrl,
        contentDescription = null,
        modifier = Modifier
          .size(14.dp)
          .clip(RoundedCornerShape(3.dp))
      )
    } else {
      Box(
        modifier = Modifier
          .size(14.dp)
          .clip(CircleShape)
          .background(SummerPrimaryContainer.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Link,
          contentDescription = null,
          tint = SummerPrimaryContainer,
          modifier = Modifier.size(10.dp)
        )
      }
    }
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = website.domain,
      style = CardLabelStyle,
      color = SummerDomain,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f, fill = false)
    )
    if (showAiBadge) {
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        color = SummerPrimaryContainer.copy(alpha = 0.15f),
        shape = CircleShape
      ) {
        Text(
          text = "AI",
          style = CardLabelStyle.copy(fontSize = 9.sp, lineHeight = 12.sp),
          color = SummerPrimaryContainer,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
      }
    }
  }
}
