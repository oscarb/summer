package se.oscarb.summer

import android.app.Activity
import android.os.Bundle
import se.oscarb.summer.data.SummerRepository
import se.oscarb.summer.util.ExternalBrowserLauncher
import se.oscarb.summer.util.QuickSaveNotificationHelper

/**
 * Zero-UI invisible Activity invoked when the user taps on the quick-save notification.
 *
 * It checks [se.oscarb.summer.data.AppSettings.removeWhenOpenedFromNotification]:
 * - If enabled, removes the link from Summer so it's not saved.
 * - Opens the link in the external browser.
 * - Immediately finishes so the transition is instantaneous.
 */
class OpenNotificationLinkActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val url = intent.getStringExtra(EXTRA_URL).orEmpty()
    val websiteId = intent.getStringExtra(EXTRA_WEBSITE_ID).orEmpty()

    QuickSaveNotificationHelper.cancelNotification(applicationContext)

    if (url.isNotBlank()) {
      val repo = SummerRepository.getInstance(applicationContext)
      val settings = repo.settings.value

      if (settings.removeWhenOpenedFromNotification && websiteId.isNotBlank()) {
        repo.deleteWebsite(websiteId)
      }

      ExternalBrowserLauncher.openInExternalBrowser(
        context = applicationContext,
        url = url,
        preferredBrowserPackage = settings.preferredBrowserPackage,
        useCustomTabs = settings.useCustomTabs
      )
    }

    finish()
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
  }

  companion object {
    const val EXTRA_URL = "extra_url"
    const val EXTRA_WEBSITE_ID = "extra_website_id"
  }
}
