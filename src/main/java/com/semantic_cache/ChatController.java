package com.semantic_cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/chat")
    public ChatResponse chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        return chatService.handle(tenantId, request);
    }
}
