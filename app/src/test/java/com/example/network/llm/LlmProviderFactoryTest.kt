package com.example.network.llm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Stage 1 tests: provider selection must not crash and must be honest about
 * availability. No network or paid API calls are made.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LlmProviderFactoryTest {

    @Test
    fun `factory returns nano provider for gemini_nano`() {
        val provider = LlmProviderFactory.create(LlmProviderFactory.PROVIDER_GEMINI_NANO)
        assertTrue(provider is GeminiNanoLlmProvider)
        assertEquals(LlmProviderFactory.PROVIDER_GEMINI_NANO, provider.id)
    }

    @Test
    fun `factory returns gemma3 provider when context is supplied`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = LlmProviderFactory.create(LlmProviderFactory.PROVIDER_GEMMA3_LOCAL, context)
        assertTrue(provider is Gemma3LocalLlmProvider)
        assertEquals(LlmProviderFactory.PROVIDER_GEMMA3_LOCAL, provider.id)
    }

    @Test
    fun `factory throws actionable error for gemma3 without context`() {
        val error = runCatching {
            LlmProviderFactory.create(LlmProviderFactory.PROVIDER_GEMMA3_LOCAL, null)
        }.exceptionOrNull()
        assertTrue("Expected IllegalStateException, got $error", error is IllegalStateException)
        assertTrue(
            "Message should name the provider: ${error?.message}",
            error?.message?.contains("Context") == true
        )
    }

    @Test
    fun `unknown provider id falls back to gemini cloud provider`() {
        val provider = LlmProviderFactory.create("does_not_exist")
        assertTrue(provider is GeminiLlmProvider)
    }

    @Test
    fun `local providers list contains nano and gemma3`() {
        val locals = LlmProviderFactory.localProviders()
        assertTrue(locals.contains(LlmProviderFactory.PROVIDER_GEMINI_NANO))
        assertTrue(locals.contains(LlmProviderFactory.PROVIDER_GEMMA3_LOCAL))
    }

    @Test
    fun `nano provider static availability is honest about sdk floor`() {
        // Static prerequisite only: isAvailable() must equal the SDK floor check.
        // The authoritative on-device check lives in probeStatus(), not here.
        val provider = GeminiNanoLlmProvider()
        val expected = android.os.Build.VERSION.SDK_INT >= GeminiNanoLlmProvider.MIN_SDK_FOR_AICORE
        assertEquals(
            "isAvailable() must mirror the SDK floor, not hardcode a value",
            expected,
            provider.isAvailable()
        )
    }

    @Test
    fun `nano status message maps every AICore state to actionable text`() {
        val statuses = listOf(0, 1, 2, 3) // UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE
        statuses.forEach { status ->
            val msg = GeminiNanoLlmProvider.statusMessage(status)
            assertTrue("Status $status produced blank message", msg.isNotBlank())
        }
        assertTrue(
            GeminiNanoLlmProvider.statusMessage(1).contains("download", ignoreCase = true)
        )
        assertTrue(
            GeminiNanoLlmProvider.statusMessage(3).contains("ready", ignoreCase = true)
        )
    }
}
