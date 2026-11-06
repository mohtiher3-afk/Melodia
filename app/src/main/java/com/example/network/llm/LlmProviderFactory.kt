package com.example.network.llm

import com.example.BuildConfig

/**
 * Central registry for LLM providers. Providers are selected by id and
 * can be switched at runtime (e.g. from Settings) without re-creating
 * network clients. Cloud provider quotas and prices can change; only the local
 * Gemini Nano provider avoids per-request API billing, subject to device support.
 */
object LlmProviderFactory {

    // Cloud providers may require a key and may incur charges after any free quota.
    const val PROVIDER_GEMINI = "gemini"
    const val PROVIDER_GEMINI_NANO = "gemini_nano"
    const val PROVIDER_GEMMA3_LOCAL = "gemma3_local"

    // Optional cloud-compatible providers; do not assume they have a free tier.
    const val PROVIDER_OPENROUTER = "openrouter"
    const val PROVIDER_HUGGINGFACE = "huggingface"
    const val PROVIDER_TOGETHER = "together"

    /**
     * Creates the appropriate LlmProvider based on the given id.
     * Cloud providers are optional. Gemini Nano runs locally when supported by AICore.
     */
    fun create(id: String, context: android.content.Context? = null): LlmProvider = when (id) {
        PROVIDER_GEMINI -> GeminiLlmProvider()
        PROVIDER_GEMINI_NANO -> GeminiNanoLlmProvider()
        PROVIDER_GEMMA3_LOCAL -> Gemma3LocalLlmProvider(context ?: throw IllegalStateException("Context required for Gemma3LocalLlmProvider"))
        PROVIDER_OPENROUTER -> OpenRouterLlmProvider(
            baseUrl = safe(BuildConfig.FREELLM_BASE_URL),
            apiKey = safeOrNull(BuildConfig.FREELLM_API_KEY),
            model = safeOrDefault(BuildConfig.FREELLM_MODEL, "anthropic/claude-3-haiku")
        )
        PROVIDER_HUGGINGFACE -> HuggingFaceLlmProvider(
            baseUrl = safe(BuildConfig.FREELLM_BASE_URL),
            apiKey = safeOrNull(BuildConfig.FREELLM_API_KEY),
            model = safeOrDefault(BuildConfig.FREELLM_MODEL, "meta-llama/Llama-3.2-1B-Instruct")
        )
        PROVIDER_TOGETHER -> TogetherLlmProvider(
            baseUrl = safe(BuildConfig.FREELLM_BASE_URL),
            apiKey = safeOrNull(BuildConfig.FREELLM_API_KEY),
            model = safeOrDefault(BuildConfig.FREELLM_MODEL, "meta-llama/Llama-3.3-70B-Instruct-Turbo")
        )
        else -> GeminiLlmProvider()
    }

    /** Only the local backend is free of per-request API charges. */
    fun localProviders(): List<String> = listOf(PROVIDER_GEMINI_NANO, PROVIDER_GEMMA3_LOCAL)

    fun availableProviders(): List<LlmProvider> =
        listOf(create(PROVIDER_GEMINI_NANO), create(PROVIDER_GEMINI), create(PROVIDER_OPENROUTER))
            .filter { it.isAvailable() }

    private fun safe(v: String?): String = v?.trim().orEmpty()
    private fun safeOrNull(v: String?): String? = v?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("MY_") }
    private fun safeOrDefault(v: String?, default: String): String =
        v?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("MY_") } ?: default
}
