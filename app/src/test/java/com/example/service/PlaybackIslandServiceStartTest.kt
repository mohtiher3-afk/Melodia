package com.example.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Stage 5 test: the playback foreground service must only start when the user
 * enabled the Island overlay. Plain in-app playback must not spawn a
 * persistent notification by itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaybackIslandServiceStartTest {

    private fun setIslandEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PlaybackIslandService.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PlaybackIslandService.PREFERENCE_ISLAND_ENABLED, enabled)
            .commit()
    }

    @Test
    fun `start does not launch the service when island is disabled`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context as android.app.Application
        setIslandEnabled(context, enabled = false)

        PlaybackIslandService.start(context)

        val shadow = shadowOf(app)
        assertEquals(
            "no service intent may be started while the Island is disabled",
            null,
            shadow.nextStartedService
        )
    }

    @Test
    fun `start launches the service when island is enabled`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context as android.app.Application
        setIslandEnabled(context, enabled = true)

        PlaybackIslandService.start(context)

        val shadow = shadowOf(app)
        val started = shadow.nextStartedService
        assertTrue(
            "the Island service must start when enabled",
            started != null && started.component?.className == PlaybackIslandService::class.java.name
        )
    }
}
