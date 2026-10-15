package com.example.passerwatch

import android.Manifest
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.passerwatch.ui.MonitorViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var vm: MonitorViewModel
    @Before fun setup() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission("com.example.passerwatch", Manifest.permission.CAMERA)
        compose.activityRule.scenario.recreate()
        compose.runOnIdle { vm = ViewModelProvider(compose.activity)[MonitorViewModel::class.java] }
        compose.waitUntil(15000) { vm.state.value.cameraReady && vm.storage.value.initialized }
    }
    @Test fun retryRestoresVisiblePreviewStream() {
        var original: PreviewView? = null
        compose.runOnIdle { original = findPreview(compose.activity.window.decorView) }
        compose.waitUntil(15000) { original?.previewStreamState?.value == PreviewView.StreamState.STREAMING }
        compose.onNodeWithText("重试相机").performClick()
        var current: PreviewView? = null
        compose.waitUntil(15000) {
            compose.runOnIdle { current = findPreview(compose.activity.window.decorView) }
            current !== original && current?.isAttachedToWindow == true && current?.previewStreamState?.value == PreviewView.StreamState.STREAMING
        }
    }
    private fun findPreview(view: View): PreviewView? {
        if (view is PreviewView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findPreview(view.getChildAt(i))?.let { return it }
        return null
    }
}
