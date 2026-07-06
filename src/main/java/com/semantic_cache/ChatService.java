package com.semantic_cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a chat request through the L1 exact-match cache:
 *
 *   build key -> L1 hit? return cached : call Gemini -> store -> return
 *
 * Keeps the decision logic out of the web layer (ChatController) and out of the
 * Gemini wrapper (GeminiClient).
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private L1Cache l1Cache;

    @Autowired
    private GeminiClient geminiClient;

    @Value("${gemini.llm-model}")
    private String defaultModel;

    public ChatResponse handle(String tenantId, ChatRequest request) {
        // Effective model: the request's model if provided, else the configured default.
        // Resolved here so the cache key reflects the model that would actually answer.
        String model = request.model() != null ? request.model() : defaultModel;

        String key = l1Cache.buildKey(
                tenantId,
                request.prompt(),
                request.systemPrompt(),
                model,
                request.temperature(),
                request.topP(),
                request.maxTokens());

        String cached = l1Cache.get(key);
        if (cached != null) {
            log.debug("L1 hit  tenant={} key={}", tenantId, key);
            return new ChatResponse(cached, true);
        }

        log.debug("L1 miss tenant={} key={} - calling Gemini", tenantId, key);
        // TODO (decision #3): forward model + generationConfig (temperature, topP,
        // maxTokens) to Gemini. For now the call uses the configured model + default
        // sampling, so those fields affect only the cache key, not the actual output.
        String text = geminiClient.generate(request.prompt());
        l1Cache.put(key, text);
        return new ChatResponse(text, false);
    }
}
