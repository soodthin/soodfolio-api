package com.soodfolio.api.service;

import com.soodfolio.api.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PublicRateLimiter {

    private static final long WINDOW_MS = 60_000L; // 60 seconds sliding window
    private static final int MAX_ENTRIES = 10_000;

    private final Map<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    /**
     * Checks rate limit for given bucket and client IP.
     * Throws RateLimitExceededException (HTTP 429) if maxRequests exceeded within 60s sliding window.
     */
    public void checkRateLimit(String bucket, String clientIp, int maxRequests) {
        String sanitizedIp = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown";
        String key = bucket + ":" + sanitizedIp;
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        // Proactive memory cleanup if approaching threshold
        if (requestLogs.size() > MAX_ENTRIES) {
            cleanupExpiredBuckets(windowStart);
        }

        Deque<Long> timestamps = requestLogs.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            // Evict expired timestamps outside the 60s window
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                throw new RateLimitExceededException("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút.");
            }

            timestamps.addLast(now);
        }
    }

    private void cleanupExpiredBuckets(long windowStart) {
        requestLogs.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() <= windowStart) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
    }

    /**
     * Resets all tracking state (used in testing).
     */
    public void reset() {
        requestLogs.clear();
    }
}
