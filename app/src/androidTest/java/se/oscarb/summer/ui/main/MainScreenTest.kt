package se.oscarb.summer.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/** UI tests for [se.oscarb.summer.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun mainScreen_rendersTitle() {
    composeTestRule.setContent { MainScreen() }
    composeTestRule.onNodeWithText("☀️ Summer").assertExists()
  }
}
