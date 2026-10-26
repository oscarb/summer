package se.oscarb.summer.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class GoogleFeedDetectorTest {

  @Test
  fun testShortenerUrlsIdentified() {
    assertTrue(GoogleFeedDetector.isGoogleFeedUrl("https://search.app/xyz123"))
    assertTrue(GoogleFeedDetector.isGoogleFeedUrl("https://news.search.app/abc456"))
    assertTrue(GoogleFeedDetector.isGoogleFeedUrl("https://share.google/def789"))
  }

  @Test
  fun testTrackingParametersIdentified() {
    assertTrue(GoogleFeedDetector.isGoogleFeedUrl("https://example.com/article?usqp=mq331AQHKAFQArABCA=="))
    assertTrue(GoogleFeedDetector.isGoogleFeedUrl("https://techcrunch.com/post?ved=2ahUKEwj...&id=123"))
    assertTrue(GoogleFeedDetector.isGoogleFeedUrl("https://www.google.com/url?q=https://example.com"))
  }

  @Test
  fun testRegularUrlsNotIdentified() {
    assertFalse(GoogleFeedDetector.isGoogleFeedUrl("https://nytimes.com/world"))
    assertFalse(GoogleFeedDetector.isGoogleFeedUrl("https://github.com/torvalds/linux?tab=readme"))
    assertFalse(GoogleFeedDetector.isGoogleFeedUrl("https://example.com/search?q=hello"))
    assertFalse(GoogleFeedDetector.isGoogleFeedUrl(null))
    assertFalse(GoogleFeedDetector.isGoogleFeedUrl(""))
  }

  @Test
  fun testReferrerDetection() {
    assertTrue(GoogleFeedDetector.isGoogleAppReferrerString("android-app://com.google.android.googlequicksearchbox/https/www.google.com"))
    assertFalse(GoogleFeedDetector.isGoogleAppReferrerString("android-app://com.android.chrome"))
    assertFalse(GoogleFeedDetector.isGoogleAppReferrerString(null))
  }
}
