package com.semantic_cache;

/**
 * Chat response. {@code cached} is true when the text came from the L1 cache
 * (Redis hit), false when it came from a fresh Gemini call. Useful for observing
 * cache behavior during testing.
 */
public record ChatResponse(String text, boolean cached) {
}
