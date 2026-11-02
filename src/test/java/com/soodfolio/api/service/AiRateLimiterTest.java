package com.soodfolio.api.service;

import com.soodfolio.api.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("AiRateLimiter Unit Tests")
class AiRateLimiterTest {

    private AiRateLimiter rateLimiter;
    private UUID userIdA;
    private UUID userIdB;

    @BeforeEach
    void setUp() {
        rateLimiter = new AiRateLimiter(15);
        userIdA = UUID.randomUUID();
        userIdB = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should allow exactly 15 requests in 1 minute without exception")
    void shouldAllow15RequestsWithinOneMinute() {
        for (int i = 1; i <= 15; i++) {
            assertDoesNotThrow(
                    () -> rateLimiter.checkRateLimit(userIdA),
                    "Request " + i + " out of 15 should succeed"
            );
        }
    }

    @Test
    @DisplayName("Should throw RateLimitExceededException (HTTP 429) on 16th request")
    void shouldThrowRateLimitExceededExceptionOn16thRequest() {
        for (int i = 1; i <= 15; i++) {
            rateLimiter.checkRateLimit(userIdA);
        }

        assertThatThrownBy(() -> rateLimiter.checkRateLimit(userIdA))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("15 yêu cầu AI mỗi phút")
                .satisfies(ex -> {
                    RateLimitExceededException rle = (RateLimitExceededException) ex;
                    assertThat(rle.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(rle.getStatus().value()).isEqualTo(429);
                });
    }

    @Test
    @DisplayName("Should maintain independent limits for different user UUIDs")
    void shouldIsolateLimitsForDifferentUsers() {
        // User A consumes all 15 permits
        for (int i = 1; i <= 15; i++) {
            rateLimiter.checkRateLimit(userIdA);
        }

        // User A's 16th request must be rejected
        assertThatThrownBy(() -> rateLimiter.checkRateLimit(userIdA))
                .isInstanceOf(RateLimitExceededException.class);

        // User B must still have full allowance (15 requests)
        for (int i = 1; i <= 15; i++) {
            assertDoesNotThrow(
                    () -> rateLimiter.checkRateLimit(userIdB),
                    "User B request " + i + " should succeed despite User A being rate limited"
            );
        }

        // User B's 16th request is rejected
        assertThatThrownBy(() -> rateLimiter.checkRateLimit(userIdB))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("Should return boolean status via tryAcquire")
    void shouldReturnBooleanStatusViaTryAcquire() {
        for (int i = 1; i <= 15; i++) {
            assertThat(rateLimiter.tryAcquire(userIdA))
                    .as("Permit %d should be acquired", i)
                    .isTrue();
        }

        assertThat(rateLimiter.tryAcquire(userIdA))
                .as("16th permit should be rejected")
                .isFalse();
    }

    @Test
    @DisplayName("Should handle null userId gracefully without rate limiting")
    void shouldHandleNullUserIdGracefully() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 20; i++) {
                rateLimiter.checkRateLimit(null);
            }
        });

        assertThat(rateLimiter.tryAcquire(null)).isTrue();
    }

    @Test
    @DisplayName("Should reset limit for specific user")
    void shouldResetLimitForSpecificUser() {
        for (int i = 0; i < 15; i++) {
            rateLimiter.checkRateLimit(userIdA);
        }
        assertThat(rateLimiter.tryAcquire(userIdA)).isFalse();

        rateLimiter.reset(userIdA);

        assertThat(rateLimiter.tryAcquire(userIdA))
                .as("After reset, user should be able to acquire permits again")
                .isTrue();
    }

    @Test
    @DisplayName("Should clear all rate limits across all users")
    void shouldClearAllUsers() {
        for (int i = 0; i < 15; i++) {
            rateLimiter.checkRateLimit(userIdA);
            rateLimiter.checkRateLimit(userIdB);
        }
        assertThat(rateLimiter.tryAcquire(userIdA)).isFalse();
        assertThat(rateLimiter.tryAcquire(userIdB)).isFalse();

        rateLimiter.clearAll();

        assertThat(rateLimiter.tryAcquire(userIdA)).isTrue();
        assertThat(rateLimiter.tryAcquire(userIdB)).isTrue();
    }

    @Test
    @DisplayName("Should respect custom configured rate limit threshold")
    void shouldRespectCustomConfiguredLimit() {
        AiRateLimiter customLimiter = new AiRateLimiter(5);
        assertThat(customLimiter.getMaxRequestsPerMinute()).isEqualTo(5);

        for (int i = 0; i < 5; i++) {
            assertThat(customLimiter.tryAcquire(userIdA)).isTrue();
        }
        assertThat(customLimiter.tryAcquire(userIdA)).isFalse();
    }

    @Test
    @DisplayName("Should be thread-safe under concurrent access")
    void shouldSupportConcurrentRequestsSafely() throws InterruptedException {
        int threads = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        UUID concurrentUserId = UUID.randomUUID();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    if (rateLimiter.tryAcquire(concurrentUserId)) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(15);
        assertThat(rejectedCount.get()).isEqualTo(15);
    }
}
