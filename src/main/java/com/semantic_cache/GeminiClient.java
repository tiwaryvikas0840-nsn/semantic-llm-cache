package com.semantic_cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final RestClient restClient;
    private final String model;

    public GeminiClient(
            @Value("${gemini.llm-model}") String model,
            @Value("${gemini.api-key}") String apiKey) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    public String generate(String prompt) {
        log.debug("Calling Gemini generateContent, model={}", model);

        var request = new GenerateContentRequest(
                List.of(new Content(List.of(new Part(prompt))))
        );

        var response = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .body(request)
                .retrieve()
                .body(GenerateContentResponse.class);

        return extractText(response);
    }

    private static String extractText(GenerateContentResponse response) {
        if (response == null || response.candidates() == null) {
            throw new IllegalStateException("Gemini returned no candidates");
        }
        return response.candidates().stream()
                .findFirst()
                .map(Candidate::content)
                .map(Content::parts)
                .flatMap(parts -> parts.stream().findFirst())
                .map(Part::text)
                .orElseThrow(() -> new IllegalStateException("Gemini returned empty response"));
    }

    private record GenerateContentRequest(List<Content> contents) {}
    private record GenerateContentResponse(List<Candidate> candidates) {}
    private record Candidate(Content content) {}
    private record Content(List<Part> parts) {}
    private record Part(String text) {}
}
