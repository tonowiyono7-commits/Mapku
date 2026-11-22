package com.example

import android.view.ViewGroup
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun findMapView(root: android.view.View): MapView? {
        if (root is MapView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findMapView(root.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    @Test
    fun mainActivityRendersMapLayersAndNavigationWithoutCrash() {
        composeRule.waitForIdle()

        val mapView = findMapView(composeRule.activity.window.decorView)
        assertNotNull("MapView must be present in view hierarchy", mapView)
        assertTrue("MapView width must be > 0", mapView!!.width > 0)
        assertTrue("MapView height must be > 0", mapView.height > 0)

        composeRule.onNodeWithTag("start_stop_mock_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("map_layers_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("layer_option_satellite").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("layer_option_terrain").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("layer_option_default").assertIsDisplayed().performClick()

        // Navigate across all tabs and back to map
        composeRule.onNodeWithTag("nav_saved").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_map").performClick()
        composeRule.waitForIdle()
    }
}
