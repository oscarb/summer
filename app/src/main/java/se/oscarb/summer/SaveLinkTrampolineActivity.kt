package se.oscarb.summer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import se.oscarb.summer.data.SaveUrlOutcome
import se.oscarb.summer.data.SummerRepository
import se.oscarb.summer.util.ExternalBrowserLauncher
import se.oscarb.summer.util.GoogleFeedDetector

/**
 * Zero-UI trampoline activity that registers as a Browser (`ACTION_VIEW` for http/https)
 * and as a Share target (`ACTION_SEND` for text/plain).
 *
 * When a user taps a link in Google Feed (or shares a link to Summer), this activity
 * saves the URL to SummerRepository, shows a quick Toast confirmation, and immediately
 * finishes so the user stays right where they were in the source app.
 *
 * If [se.oscarb.summer.data.AppSettings.onlyInterceptGoogleFeed] is enabled, links that do
 * NOT originate from Google Feed will be forwarded directly to the chosen default external browser,
 * behaving as if Summer were not installed.
 */
class SaveLinkTrampolineActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val incomingUrl = extractUrlFromIntent(intent)
    val repo = SummerRepository.getInstance(applicationContext)
    val settings = repo.settings.value

    if (incomingUrl != null) {
      // If "Only Intercept Google Feed URLs" is enabled, verify origin
      if (settings.onlyInterceptGoogleFeed) {
        val isFeed = GoogleFeedDetector.isFromGoogleFeed(
          intent = intent,
          url = incomingUrl,
          referrer = referrer
        )

        if (!isFeed) {
          // Pass-through to chosen external browser / default browser
          ExternalBrowserLauncher.openInExternalBrowser(
            context = applicationContext,
            url = incomingUrl,
            preferredBrowserPackage = settings.preferredBrowserPackage,
            useCustomTabs = settings.useCustomTabs
          )
          finish()
          @Suppress("DEPRECATION")
          overridePendingTransition(0, 0)
          return
        }
      }

      when (val outcome = repo.saveOrBumpUrl(incomingUrl)) {
        is SaveUrlOutcome.SavedNew -> {
          Toast.makeText(
            applicationContext,
            "☀️ Saved to Summer: ${outcome.domain}",
            Toast.LENGTH_SHORT
          ).show()
        }
        is SaveUrlOutcome.BumpedExisting -> {
          Toast.makeText(
            applicationContext,
            "☀️ Moved to top in Summer: ${outcome.domain}",
            Toast.LENGTH_SHORT
          ).show()
        }
        SaveUrlOutcome.InvalidUrl -> {
          Toast.makeText(
            applicationContext,
            "Summer: Could not find a valid http/https link",
            Toast.LENGTH_SHORT
          ).show()
        }
      }
    } else {
      Toast.makeText(
        applicationContext,
        "Summer: No link found to save",
        Toast.LENGTH_SHORT
      ).show()
    }

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
