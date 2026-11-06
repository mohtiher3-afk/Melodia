package com.example.network.llm

import com.example.BuildConfig

/**
 * Central registry for LLM providers. Providers are selected by id and
 * can be switched at runtime (e.g. from Settings) without re-creating
 * network clients.
 *
 * Free API tiers are available - no payment required ever.
 */
object LlmProviderFactory {

    // ✅ Built-in Gemini (may have quotas, check Google Cloud)
    const val PROVIDER_GEMINI = "gemini"

    // ✅ FreeLLM providers - completely free tiers available
    const val PROVIDER_OPENROUTER = "openrouter"      // $5 free credit, never expires
    const val PROVIDER_HUGGINGFACE = "huggingface"    // Free tier, email signup
    const val PROVIDER_TOGETHER = "together"          // 1M tokens free

    /**
     * Creates the appropriate LlmProvider based on the given id.
     * Supports both paid and free API tiers.
     */
    fun create(id: String): LlmProvider = when (id) {
        PROVIDER_GEMINI -> GeminiLlmProvider()
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

    /**
     * Returns a list of providers that have free tiers available.
     * Useful for showing "Free options" in Settings UI.
     */
    fun freeProviders(): List<String> = listOf(
        PROVIDER_OPENROUTER,
        PROVIDER_HUGGINGFACE,
        PROVIDER_TOGETHER,
        PROVIDER_GEMINI
    )

    fun availableProviders(): List<LlmProvider> =
        listOf(create(PROVIDER_GEMINI), create(PROVIDER_OPENROUTER)).filter { it.isAvailable() }

    private fun safe(v: String?): String = v?.trim().orEmpty()
    private fun safeOrNull(v: String?): String? = v?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("MY_") }
    private fun safeOrDefault(v: String?, default: String): String =
        v?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("MY_") } ?: default
}
