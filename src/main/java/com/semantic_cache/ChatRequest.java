package com.semantic_cache;

/**
 * A chat request. Only {@code prompt} is required; the rest are optional and
 * default to stable values when building the L1 cache key. tenant_id is NOT here —
 * it arrives as the X-Tenant-Id header (routing/identity metadata, not payload).
 *
 * Note: model/temperature/topP/maxTokens are currently used only in the cache key,
 * not yet forwarded to Gemini (see ChatService). They live here now so the request
 * contract already matches the final design.
 */
public record ChatRequest(
        String prompt,
        String systemPrompt,
        String model,
        Double temperature,
        Double topP,
        Integer maxTokens
) {
}
