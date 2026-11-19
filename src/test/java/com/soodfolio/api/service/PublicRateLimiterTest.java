package com.soodfolio.api.service;

import com.soodfolio.api.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("PublicRateLimiter Unit Tests")
class PublicRateLimiterTest {

    private PublicRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new PublicRateLimiter();
        rateLimiter.reset();
    }

    @Test
    @DisplayName("Should allow requests within configured limit")
    void shouldAllowRequestsWithinConfiguredLimit() {
        String ip = "192.168.1.10";
        int max = 5;

        for (int i = 1; i <= max; i++) {
            assertDoesNotThrow(
                    () -> rateLimiter.checkRateLimit("login", ip, max),
                    "Request " + i + " should succeed"
            );
        }
    }

    @Test
    @DisplayName("Should throw RateLimitExceededException when limit is exceeded")
    void shouldThrowRateLimitExceededExceptionWhenLimitExceeded() {
        String ip = "192.168.1.20";
        int max = 5;

        for (int i = 1; i <= max; i++) {
            rateLimiter.checkRateLimit("login", ip, max);
        }

        assertThatThrownBy(() -> rateLimiter.checkRateLimit("login", ip, max))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút.")
                .satisfies(ex -> {
                    RateLimitExceededException rle = (RateLimitExceededException) ex;
                    assertThat(rle.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(rle.getStatus().value()).isEqualTo(429);
                });
    }

    @Test
    @DisplayName("Should isolate limits by client IP")
    void shouldIsolateLimitsByClientIp() {
        String ipA = "10.0.0.1";
        String ipB = "10.0.0.2";
        int max = 3;

        // Exhaust quota for IP A
        for (int i = 1; i <= max; i++) {
            rateLimiter.checkRateLimit("views", ipA, max);
        }

        // IP A is blocked
        assertThatThrownBy(() -> rateLimiter.checkRateLimit("views", ipA, max))
                .isInstanceOf(RateLimitExceededException.class);

        // IP B can still make all requests
        for (int i = 1; i <= max; i++) {
            assertDoesNotThrow(
                    () -> rateLimiter.checkRateLimit("views", ipB, max),
                    "IP B request " + i + " should succeed"
            );
        }

        // IP B is blocked on next request
        assertThatThrownBy(() -> rateLimiter.checkRateLimit("views", ipB, max))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("Should isolate limits by bucket for same IP")
    void shouldIsolateLimitsByBucketForSameIp() {
        String ip = "172.16.0.5";

        // Exhaust login bucket (limit 2)
        rateLimiter.checkRateLimit("login", ip, 2);
        rateLimiter.checkRateLimit("login", ip, 2);
        assertThatThrownBy(() -> rateLimiter.checkRateLimit("login", ip, 2))
                .isInstanceOf(RateLimitExceededException.class);

        // Forgot-password bucket (limit 5) should still be available
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit("forgot-password", ip, 5));
    }

    @Test
    @DisplayName("Should normalize null or blank client IP to unknown")
    void shouldNormalizeNullOrBlankClientIpToUnknown() {
        int max = 2;

        rateLimiter.checkRateLimit("telemetry", null, max);
        rateLimiter.checkRateLimit("telemetry", "   ", max);

        assertThatThrownBy(() -> rateLimiter.checkRateLimit("telemetry", "", max))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("Should enforce thread safety under concurrent requests")
    void shouldEnforceThreadSafetyUnderConcurrentRequests() throws Exception {
        String ip = "203.0.113.50";
        int limit = 15;
        int totalThreads = 30;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < totalThreads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    rateLimiter.checkRateLimit("concurrency-bucket", ip, limit);
                    successCount.incrementAndGet();
                } catch (RateLimitExceededException e) {
                    rejectedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        // Wait for all threads to be ready, then trigger simultaneously
        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(successCount.get())
                .as("Exactly %d requests should succeed under concurrency", limit)
                .isEqualTo(limit);
        assertThat(rejectedCount.get())
                .as("Exactly %d requests should be rejected under concurrency", totalThreads - limit)
                .isEqualTo(totalThreads - limit);
    }

    @Test
    @DisplayName("Should reset all state when reset is called")
    void shouldResetAllStateWhenResetCalled() {
        String ip = "192.168.1.99";
        rateLimiter.checkRateLimit("login", ip, 1);

        assertThatThrownBy(() -> rateLimiter.checkRateLimit("login", ip, 1))
                .isInstanceOf(RateLimitExceededException.class);

        rateLimiter.reset();

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit("login", ip, 1));
    }
}
