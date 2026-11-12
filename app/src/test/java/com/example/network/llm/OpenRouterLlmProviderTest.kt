package com.example.network.llm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Stage 5 tests: cloud LLM base URLs must be HTTPS-only so the API key in the
 * Authorization header can never travel in cleartext. No network calls made.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenRouterLlmProviderTest {

    @Test
    fun `plain http base url is not available`() {
        val provider = OpenRouterLlmProvider(
            baseUrl = "http://insecure.example.com/v1",
            apiKey = "k",
            model = "m"
        )
        assertFalse(provider.isAvailable())
    }

    @Test
    fun `https base url is available`() {
        val provider = OpenRouterLlmProvider(
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = "k",
            model = "m"
        )
        assertTrue(provider.isAvailable())
    }

    @Test
    fun `blank base url is not available`() {
        val provider = OpenRouterLlmProvider(baseUrl = "", apiKey = null, model = "m")
        assertFalse(provider.isAvailable())
    }
}
