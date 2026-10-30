package se.oscarb.summer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.oscarb.summer.data.SaveUrlOutcome
import se.oscarb.summer.data.SummerRepository
import se.oscarb.summer.podcast.PodcastPlaybackController
import se.oscarb.summer.theme.SummerTheme
import se.oscarb.summer.util.ExternalBrowserLauncher
import se.oscarb.summer.util.GoogleFeedDetector

/**
 * Translucent trampoline overlay activity that registers as a Browser (`ACTION_VIEW` for http/https)
 * and as a Share target (`ACTION_SEND` for text/plain).
 *
 * When a user taps a link in Google Feed (or shares a link to Summer), this activity
 * saves the URL to SummerRepository and displays a compact, translucent bottom-aligned
 * snackbar overlay with quick actions to Open App, Open Link directly, or generate a
 * Catch Me Up Podcast silently in the background. Tapping outside or waiting 4.5s dismisses
 * immediately so the user stays right where they were in the source app.
 *
 * If [se.oscarb.summer.data.AppSettings.onlyInterceptGoogleFeed] is enabled, links that do
 * NOT originate from Google Feed will be forwarded directly to the chosen default external browser,
 * behaving as if Summer were not installed.
 */
class SaveLinkTrampolineActivity : ComponentActivity() {

  private var actionHandled = false
  private var savedUrl: String? = null
  private var savedDomain: String? = null
  private var wasBumped: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    suppressTransitions()

    val repo = SummerRepository.getInstance(applicationContext)
    val settings = repo.settings.value

    val restoredUrl = savedInstanceState?.getString(KEY_SAVED_URL)
    val restoredDomain = savedInstanceState?.getString(KEY_SAVED_DOMAIN)
    val restoredBumped = savedInstanceState?.getBoolean(KEY_WAS_BUMPED) ?: false

    if (restoredUrl != null && restoredDomain != null) {
      savedUrl = restoredUrl
      savedDomain = restoredDomain
      wasBumped = restoredBumped
    } else {
      val incomingUrl = extractUrlFromIntent(intent)
      if (incomingUrl == null) {
        Toast.makeText(
          applicationContext,
          "Summer: No link found to save",
          Toast.LENGTH_SHORT
        ).show()
        finishSilently()
        return
      }

      // If "Only Intercept Google Feed URLs" is enabled, verify origin
      if (settings.onlyInterceptGoogleFeed) {
        val isFeed = GoogleFeedDetector.isFromGoogleFeed(
          intent = intent,
          url = incomingUrl,
          referrer = referrer
        )

        if (!isFeed) {
          // Pass-through to chosen external browser / default browser with zero UI
          ExternalBrowserLauncher.openInExternalBrowser(
            context = applicationContext,
            url = incomingUrl,
            preferredBrowserPackage = settings.preferredBrowserPackage,
            useCustomTabs = settings.useCustomTabs
          )
          finishSilently()
          return
        }
      }

      when (val outcome = repo.saveOrBumpUrl(incomingUrl)) {
        is SaveUrlOutcome.SavedNew -> {
          savedUrl = outcome.url.ifBlank { SummerRepository.normalizeUrl(incomingUrl) ?: incomingUrl }
          savedDomain = outcome.domain
          wasBumped = false
        }
        is SaveUrlOutcome.BumpedExisting -> {
          savedUrl = outcome.url.ifBlank { SummerRepository.normalizeUrl(incomingUrl) ?: incomingUrl }
          savedDomain = outcome.domain
          wasBumped = true
        }
        SaveUrlOutcome.InvalidUrl -> {
          Toast.makeText(
            applicationContext,
            "Summer: Could not find a valid http/https link",
            Toast.LENGTH_SHORT
          ).show()
          finishSilently()
          return
        }
      }
    }

    val activeUrl = savedUrl ?: run {
      finishSilently()
      return
    }
    val activeDomain = savedDomain ?: run {
      finishSilently()
      return
    }
    val activeWasBumped = wasBumped

    configureTransparentEdgeToEdgeWindow()

    setContent {
      SummerTheme(darkTheme = true, dynamicColor = true) {
        PostSaveSnackbarOverlay(
          domain = activeDomain,
          wasBumped = activeWasBumped,
          onOpenApp = { handleOpenApp() },
          onOpenLink = { handleOpenLink(activeUrl) },
          onGeneratePodcast = { handleGeneratePodcast() },
          onDismiss = { handleDismiss() }
        )
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    savedUrl?.let { outState.putString(KEY_SAVED_URL, it) }
    savedDomain?.let { outState.putString(KEY_SAVED_DOMAIN, it) }
    outState.putBoolean(KEY_WAS_BUMPED, wasBumped)
  }

  private fun handleDismiss() {
    if (actionHandled) return
    actionHandled = true
    finishSilently()
  }

  private fun handleOpenApp() {
    if (actionHandled) return
    actionHandled = true
    val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
      addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
          Intent.FLAG_ACTIVITY_CLEAR_TOP or
          Intent.FLAG_ACTIVITY_SINGLE_TOP
      )
    }
    startActivity(openAppIntent)
    finishSilently()
  }

  private fun handleOpenLink(url: String) {
    if (actionHandled) return
    actionHandled = true
    val currentSettings = SummerRepository.getInstance(applicationContext).settings.value
    ExternalBrowserLauncher.openInExternalBrowser(
      context = applicationContext,
      url = url,
      preferredBrowserPackage = currentSettings.preferredBrowserPackage,
      useCustomTabs = currentSettings.useCustomTabs
    )
    finishSilently()
  }

  private fun handleGeneratePodcast() {
    if (actionHandled) return
    actionHandled = true

    val appCtx = applicationContext
    val repo = SummerRepository.getInstance(appCtx)
    val playbackController = PodcastPlaybackController.getInstance(appCtx)
    val kickoffJobKey = "trampoline-digest:${System.currentTimeMillis()}"

    // Promote foreground service with a silent notification while this overlay activity is foregrounded
    playbackController.setGenerationProgress(
      jobKey = kickoffJobKey,
      title = "Catch Me Up Podcast",
      statusText = "Preparing unread articles..."
    )

    backgroundPodcastScope.launch {
      try {
        repo.generateCatchMeUpDigestForUnread()
          .onSuccess {
            // Allow generatePodcastForDigestInternal's coroutine to register its own jobKey
            // before clearing kickoffJobKey so the silent foreground service stays seamless.
            delay(350L)
          }
          .onFailure { err ->
            withContext(Dispatchers.Main) {
              Toast.makeText(
                appCtx,
                "Summer podcast failed: ${err.message ?: "Could not start podcast generation"}",
                Toast.LENGTH_LONG
              ).show()
            }
          }
      } finally {
        playbackController.clearGenerationProgress(kickoffJobKey)
      }
    }

    finishSilently()
  }

  private fun configureTransparentEdgeToEdgeWindow() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    @Suppress("DEPRECATION")
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
    window.setDimAmount(0f)
    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }
  }

  private fun suppressTransitions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
      overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
  }

  private fun finishSilently() {
    if (!isFinishing) {
      finish()
    }
    suppressTransitions()
  }

  private fun extractUrlFromIntent(intent: Intent?): String? {
    if (intent == null) return null
    return when (intent.action) {
      Intent.ACTION_VIEW -> intent.dataString
      Intent.ACTION_SEND -> {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
          ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
        SummerRepository.extractFirstHttpUrl(sharedText) ?: sharedText
      }
      else -> intent.dataString
    }
  }

  companion object {
    private const val KEY_SAVED_URL = "saved_url"
    private const val KEY_SAVED_DOMAIN = "saved_domain"
    private const val KEY_WAS_BUMPED = "was_bumped"

    private val backgroundPodcastScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }
}

private const val AUTO_DISMISS_COUNTDOWN_MS = 4380
private const val AUTO_DISMISS_EXIT_MS = 120L

@Composable
private fun PostSaveSnackbarOverlay(
  domain: String,
  wasBumped: Boolean,
  onOpenApp: () -> Unit,
  onOpenLink: () -> Unit,
  onGeneratePodcast: () -> Unit,
  onDismiss: () -> Unit
) {
  BackHandler(onBack = onDismiss)

  val visibilityState = remember {
    MutableTransitionState(false).apply { targetState = true }
  }
  val countdownProgress = remember { Animatable(1f) }

  LaunchedEffect(Unit) {
    countdownProgress.animateTo(
      targetValue = 0f,
      animationSpec = tween(
        durationMillis = AUTO_DISMISS_COUNTDOWN_MS,
        easing = LinearEasing
      )
    )
    visibilityState.targetState = false
    delay(AUTO_DISMISS_EXIT_MS)
    onDismiss()
  }

  val headerText = if (wasBumped) {
    "☀️ Moved to top • $domain"
  } else {
    "☀️ Saved to Summer • $domain"
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss
      ),
    contentAlignment = Alignment.BottomCenter
  ) {
    AnimatedVisibility(
      visibleState = visibilityState,
      enter = slideInVertically(
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        initialOffsetY = { fullHeight -> fullHeight / 2 }
      ) + fadeIn(animationSpec = tween(durationMillis = 140)),
      exit = slideOutVertically(
        animationSpec = tween(durationMillis = 120),
        targetOffsetY = { fullHeight -> fullHeight / 3 }
      ) + fadeOut(animationSpec = tween(durationMillis = 110))
    ) {
      Surface(
        modifier = Modifier
          .navigationBarsPadding()
          .padding(16.dp)
          .widthIn(max = 520.dp)
          .fillMaxWidth()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { /* Consume taps inside the card so they don't trigger outside dismiss */ }
          ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 10.dp,
        border = BorderStroke(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = headerText,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            FilledTonalButton(
              onClick = onOpenApp,
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Open App",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            OutlinedButton(
              onClick = onOpenLink,
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
              ),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
              ),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Open Link",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            FilledTonalButton(
              onClick = onGeneratePodcast,
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
              ),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.Podcasts,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Podcast",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          // Subtle 4.5s auto-dismiss countdown progress line
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(2.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(countdownProgress.value.coerceIn(0f, 1f))
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            )
          }
        }
      }
    }
  }
}
