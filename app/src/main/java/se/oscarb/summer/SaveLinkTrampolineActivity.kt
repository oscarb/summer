package se.oscarb.summer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import se.oscarb.summer.data.SaveUrlOutcome
import se.oscarb.summer.data.SummerRepository
import se.oscarb.summer.util.ExternalBrowserLauncher
import se.oscarb.summer.util.GoogleFeedDetector
import se.oscarb.summer.util.QuickSaveNotificationHelper

/**
 * Zero-UI trampoline activity that registers as a Browser (`ACTION_VIEW` for http/https)
 * and as a Share target (`ACTION_SEND` for text/plain).
 *
 * When a user taps a link in Google Feed (or shares a link to Summer), this activity
 * saves the URL to SummerRepository, triggers an immediate silent heads-up notification with
 * quick actions (tap opens link in browser, swipe cancels save), and immediately finishes in `onCreate`
 * so the user stays right where they were in the source app without intercepting touches or blocking scrolling.
 *
 * If [se.oscarb.summer.data.AppSettings.onlyInterceptGoogleFeed] is enabled, links that do
 * NOT originate from Google Feed will be forwarded directly to the chosen default external browser,
 * behaving as if Summer were not installed.
 */
class SaveLinkTrampolineActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val incomingUrl = extractUrlFromIntent(intent)
    if (incomingUrl != null) {
      val repo = SummerRepository.getInstance(applicationContext)
      val settings = repo.settings.value

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
          QuickSaveNotificationHelper.showPostSaveNotification(
            context = applicationContext,
            url = outcome.url.ifBlank { incomingUrl },
            domain = outcome.domain,
            websiteId = outcome.websiteId,
            wasBumped = false,
            preferredBrowserPackage = settings.preferredBrowserPackage,
            useCustomTabs = settings.useCustomTabs
          )
        }
        is SaveUrlOutcome.BumpedExisting -> {
          QuickSaveNotificationHelper.showPostSaveNotification(
            context = applicationContext,
            url = outcome.url.ifBlank { incomingUrl },
            domain = outcome.domain,
            websiteId = outcome.websiteId,
            wasBumped = true,
            preferredBrowserPackage = settings.preferredBrowserPackage,
            useCustomTabs = settings.useCustomTabs
          )
        }
        SaveUrlOutcome.InvalidUrl -> {
          // Ignore invalid URLs silently
        }
      }
    } else {
      // No link found, finish silently
    }

    finishSilently()
  }

  private fun finishSilently() {
    finish()
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
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
}
