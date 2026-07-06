package com.semantic_cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * L1 exact-match cache.
 *
 * Key   = SHA-256 of (tenant_id, prompt, system_prompt, model, temperature, top_p, max_tokens)
 * Value = the response text
 *
 * A hit means we have answered this EXACT request before, so we can skip the LLM
 * call entirely. All Redis and hashing details live here; ChatService just calls
 * buildKey / get / put.
 */
@Service
public class L1Cache {

    private static final String KEY_PREFIX = "l1:";
    private static final Duration TTL = Duration.ofHours(1);

    // ASCII Unit Separator: effectively never appears in real prompts. Joining
    // fields with it prevents key collisions between different field splits
    // (e.g. ("ab","c") vs ("a","bc") would both be "ab|c" with a naive separator).
    private static final String SEP = "";

    private final StringRedisTemplate redis;

    public L1Cache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Builds the deterministic Redis key for an exact request. */
    public String buildKey(String tenantId, String prompt, String systemPrompt,
                           String model, Double temperature, Double topP, Integer maxTokens) {
        String canonical = String.join(SEP,
                nz(tenantId),
                nz(prompt),
                nz(systemPrompt),
                nz(model),
                String.valueOf(temperature),
                String.valueOf(topP),
                String.valueOf(maxTokens));
        return KEY_PREFIX + sha256Hex(canonical);
    }

    /** Returns the cached response text, or null on a miss. */
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    /** Stores the response text under the L1 TTL. */
    public void put(String key, String value) {
        redis.opsForValue().set(key, value, TTL);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed on every JVM; this branch is unreachable.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
