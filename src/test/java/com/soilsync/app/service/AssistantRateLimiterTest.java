package com.soilsync.app.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantRateLimiterTest {

    @Test
    void enforcesTheConfiguredPerClientWindowWithoutBlockingOtherClients() {
        AssistantRateLimiter rateLimiter = new AssistantRateLimiter(2);

        assertThat(rateLimiter.tryAcquire("client-one")).isTrue();
        assertThat(rateLimiter.tryAcquire("client-one")).isTrue();
        assertThat(rateLimiter.tryAcquire("client-one")).isFalse();
        assertThat(rateLimiter.tryAcquire("client-two")).isTrue();
    }
}
