package se.oscarb.summer.ui.main

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test
import se.oscarb.summer.data.SummerRepository
import se.oscarb.summer.data.WebPageScraper

class MainScreenViewModelTest {
  @Test
  fun extractFirstHttpUrl_extractsUrlFromSharedText() {
    val shared = "Check out this article from Google Feed: https://developer.android.com/jetpack/compose/navigation!"
    val extracted = SummerRepository.extractFirstHttpUrl(shared)
    assertEquals("https://developer.android.com/jetpack/compose/navigation", extracted)
  }

  @Test
  fun normalizeUrl_addsHttpsWhenMissing() {
    val normalized = SummerRepository.normalizeUrl("example.com/news")
    assertNotNull(normalized)
    assertEquals("https://example.com/news", normalized)
  }

  @Test
  fun normalizeUrl_returnsNullForInvalidText() {
    assertNull(SummerRepository.normalizeUrl("not a url at all"))
  }

  @Test
  fun extractDomain_stripsWww() {
    assertEquals("theverge.com", WebPageScraper.extractDomain("https://www.theverge.com/2026/tech"))
  }
}
