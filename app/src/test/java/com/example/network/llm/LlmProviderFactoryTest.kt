package com.example.network.llm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Caches
import com.google.mlkit.genai.prompt.CountTokensResponse
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.GenerateTypedContentRequest
import com.google.mlkit.genai.prompt.GenerateTypedContentResponse
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.common.FeatureStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService

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

    @Test
    fun `nano checkReady gates on real AICore status not the static check`() = runTest {
        // On Robolectric API 34 the static check passes, but the real AICore status
        // is UNAVAILABLE — checkReady() must report the truth, not the SDK floor.
        GeminiNanoLlmProvider.setClientForTest(FakeGenerativeModel(FeatureStatus.UNAVAILABLE))
        try {
            val provider = GeminiNanoLlmProvider()
            assertTrue("static check should pass on API 34", provider.isAvailable())
            assertFalse("real AICore status is UNAVAILABLE", provider.checkReady())
        } finally {
            GeminiNanoLlmProvider.setClientForTest(null)
        }
    }

    @Test
    fun `nano checkReady returns true when AICore reports AVAILABLE`() = runTest {
        GeminiNanoLlmProvider.setClientForTest(FakeGenerativeModel(FeatureStatus.AVAILABLE))
        try {
            val provider = GeminiNanoLlmProvider()
            assertTrue(provider.checkReady())
        } finally {
            GeminiNanoLlmProvider.setClientForTest(null)
        }
    }

    @Test
    fun `default checkReady falls back to isAvailable`() = runTest {
        val provider = object : LlmProvider {
            override val id = "fake"
            override val displayName = "Fake"
            override fun isAvailable() = true
            override suspend fun generate(
                prompt: String,
                systemPrompt: String?,
                temperature: Float,
                maxTokens: Int
            ) = throw UnsupportedOperationException()
        }
        assertTrue(provider.checkReady())
    }
}

/**
 * Minimal GenerativeModel fake: only checkStatus() is meaningful for the
 * availability gate; every other member is a stub that must never be reached
 * by checkReady()/probeStatus().
 */
private class FakeGenerativeModel(private val status: Int) : GenerativeModel {
    override val caches: Caches get() = TODO()
    override suspend fun getBaseModelName(): String = TODO()
    override suspend fun checkStatus(): Int = status
    override suspend fun isCachingFeatureAvailable(): Boolean = TODO()
    override suspend fun isStructuredOutputFeatureAvailable(): Boolean = TODO()
    override suspend fun isSystemPromptAvailable(): Boolean = TODO()
    override suspend fun isThinkingModeAvailable(): Boolean = TODO()
    override fun download(): Flow<DownloadStatus> = TODO()
    override suspend fun warmup() = TODO()
    override suspend fun countTokens(request: GenerateContentRequest): CountTokensResponse = TODO()
    override suspend fun <T : Any> countTokens(request: GenerateTypedContentRequest<T>): CountTokensResponse = TODO()
    override suspend fun getTokenLimit(): Int = TODO()
    override suspend fun generateContent(request: GenerateContentRequest): GenerateContentResponse = TODO()
    override suspend fun generateContent(
        request: GenerateContentRequest,
        streamingCallback: StreamingCallback
    ): GenerateContentResponse = TODO()
    override suspend fun <T : Any> generateContent(request: GenerateTypedContentRequest<T>): GenerateTypedContentResponse<T> = TODO()
    override fun generateContentStream(request: GenerateContentRequest): Flow<GenerateContentResponse> = TODO()
    override suspend fun clearImplicitCaches() = TODO()
    override fun close() = TODO()
}
