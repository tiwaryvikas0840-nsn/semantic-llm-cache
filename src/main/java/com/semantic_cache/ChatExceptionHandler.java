package com.semantic_cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class ChatExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatExceptionHandler.class);

    // Gemini answered with an error status (bad key, bad request, rate limit, upstream 5xx).
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamError(RestClientResponseException ex) {
        if (ex.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            log.warn("Gemini rate limit hit: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Upstream rate limit exceeded, please retry later"));
        }
        log.error("Gemini rejected the request ({}): {}", ex.getStatusCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("Upstream LLM request failed"));
    }

    // Never reached Gemini: DNS failure, timeout, connection refused.
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleNetworkError(ResourceAccessException ex) {
        log.error("Could not reach Gemini: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Upstream LLM service unavailable"));
    }

    // Gemini replied 200 but with no usable content.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleEmptyResponse(IllegalStateException ex) {
        log.error("Gemini returned an unusable response: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("Upstream LLM returned an empty response"));
    }

    public record ErrorResponse(String error) {}
}
