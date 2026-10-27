package com.soilsync.app.controller;

import com.soilsync.app.model.AssistantChatRequest;
import com.soilsync.app.model.AssistantChatResponse;
import com.soilsync.app.service.AssistantChatLogService;
import com.soilsync.app.service.AssistantChatService;
import com.soilsync.app.service.AssistantRateLimiter;
import com.soilsync.app.service.PrivacyConsentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/assistant")
public class AssistantChatController {

    private final AssistantChatService assistantChatService;
    private final AssistantChatLogService chatLogService;
    private final AssistantRateLimiter rateLimiter;

    public AssistantChatController(
        AssistantChatService assistantChatService,
        AssistantChatLogService chatLogService,
        AssistantRateLimiter rateLimiter
    ) {
        this.assistantChatService = assistantChatService;
        this.chatLogService = chatLogService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(
        @Valid @RequestBody AssistantChatRequest request,
        @CookieValue(name = PrivacyConsentService.COOKIE_NAME, required = false) String consentToken,
        HttpServletRequest servletRequest
    ) {
        if (!rateLimiter.tryAcquire(servletRequest.getRemoteAddr())) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Assistant rate limit reached. Try again after the current one-minute window."
            );
        }

        AssistantChatService.ChatResult result = assistantChatService.chat(request);
        Optional<Long> chatLogId = chatLogService.recordIfConsented(
            consentToken,
            request,
            result.response(),
            result.weather()
        );
        return chatLogId.map(result.response()::withChatLogId).orElse(result.response());
    }
}
