package com.soilsync.app.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AssistantRateLimiter {

    private final Cache<String, Window> windows = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(2))
        .maximumSize(10_000)
        .build();
    private final int requestsPerMinute;

    public AssistantRateLimiter(
        @Value("${soilsync.assistant.rate-limit-per-minute:10}") int requestsPerMinute
    ) {
        if (requestsPerMinute < 1) {
            throw new IllegalArgumentException("The assistant rate limit must be at least one request per minute.");
        }
        this.requestsPerMinute = requestsPerMinute;
    }

    public boolean tryAcquire(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "unknown-client" : clientKey;
        long now = System.nanoTime();
        AtomicBoolean accepted = new AtomicBoolean();
        windows.asMap().compute(key, (ignored, current) -> {
            if (current == null || now - current.windowStartedAt() >= Duration.ofMinutes(1).toNanos()) {
                accepted.set(true);
                return new Window(now, 1);
            }
            if (current.requestCount() >= requestsPerMinute) {
                return current;
            }
            accepted.set(true);
            return new Window(current.windowStartedAt(), current.requestCount() + 1);
        });
        return accepted.get();
    }

    private record Window(long windowStartedAt, int requestCount) {
    }
}
