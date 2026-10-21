package se.oscarb.summer.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

data class ExternalBrowserOption(
  val packageName: String,
  val label: String
)

object ExternalBrowserLauncher {

  private val TEST_BROWSER_PROBE_URI: Uri = Uri.parse("https://www.example.com")

  fun getInstalledExternalBrowsers(context: Context): List<ExternalBrowserOption> {
    val pm = context.packageManager
    val selfPackage = context.packageName
    val viewIntent = Intent(Intent.ACTION_VIEW, TEST_BROWSER_PROBE_URI).apply {
      addCategory(Intent.CATEGORY_BROWSABLE)
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PackageManager.MATCH_ALL
    } else {
      PackageManager.MATCH_DEFAULT_ONLY
    }

    val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.queryIntentActivities(viewIntent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
      @Suppress("DEPRECATION")
      pm.queryIntentActivities(viewIntent, flags)
    }

    return resolveInfos
      .mapNotNull { info ->
        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
        if (pkg == selfPackage) return@mapNotNull null
        val label = info.loadLabel(pm).toString().ifBlank { pkg }
        ExternalBrowserOption(packageName = pkg, label = label)
      }
      .distinctBy { it.packageName }
      .sortedWith(compareBy({ !it.packageName.contains("chrome", ignoreCase = true) }, { it.label }))
  }

  fun openInExternalBrowser(
    context: Context,
    url: String,
    preferredBrowserPackage: String = ""
  ) {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    val installedBrowsers = getInstalledExternalBrowsers(context)

    val targetPkg = preferredBrowserPackage
      .takeIf { pref -> pref.isNotBlank() && installedBrowsers.any { it.packageName == pref } }
      ?: installedBrowsers.firstOrNull()?.packageName

    try {
      if (targetPkg != null) {
        val customTabsIntent = CustomTabsIntent.Builder()
          .setShowTitle(true)
          .build()
        customTabsIntent.intent.setPackage(targetPkg)
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(context, uri)
        return
      }
    } catch (_: Exception) {
      // Fall through to standard Intent.ACTION_VIEW with package or chooser
    }

    try {
      if (targetPkg != null) {
        val directIntent = Intent(Intent.ACTION_VIEW, uri).apply {
          setPackage(targetPkg)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(directIntent)
      } else {
        Toast.makeText(
          context,
          "No other browser app found on this device to open $url",
          Toast.LENGTH_LONG
        ).show()
      }
    } catch (e: Exception) {
      Toast.makeText(
        context,
        "Unable to launch external browser: ${e.message}",
        Toast.LENGTH_SHORT
      ).show()
    }
  }
}
