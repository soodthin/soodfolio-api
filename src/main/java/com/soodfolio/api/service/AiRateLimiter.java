package com.soodfolio.api.service;

import com.soodfolio.api.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AiRateLimiter {

    private final int maxRequestsPerMinute;
    private final ConcurrentMap<UUID, UserRequestWindow> userWindows = new ConcurrentHashMap<>();

    public AiRateLimiter(@Value("${app.gemini.rate-limit-per-minute:15}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public void checkRateLimit(UUID userId) {
        if (userId == null) {
            return;
        }
        UserRequestWindow window = userWindows.computeIfAbsent(userId, id -> new UserRequestWindow());
        if (!window.tryAcquire(maxRequestsPerMinute, 60_000L)) {
            throw new RateLimitExceededException("Bạn đã vượt quá giới hạn " + maxRequestsPerMinute + " yêu cầu AI mỗi phút. Vui lòng chờ giây lát.");
        }
    }

    public boolean tryAcquire(UUID userId) {
        if (userId == null) {
            return true;
        }
        UserRequestWindow window = userWindows.computeIfAbsent(userId, id -> new UserRequestWindow());
        return window.tryAcquire(maxRequestsPerMinute, 60_000L);
    }

    public void reset(UUID userId) {
        if (userId != null) {
            userWindows.remove(userId);
        }
    }

    public void clearAll() {
        userWindows.clear();
    }

    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }

    private static class UserRequestWindow {
        private final Deque<Long> timestamps = new ArrayDeque<>();

        public synchronized boolean tryAcquire(int maxLimit, long windowDurationMs) {
            long now = System.currentTimeMillis();
            long cutoff = now - windowDurationMs;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() < maxLimit) {
                timestamps.addLast(now);
                return true;
            }
            return false;
        }
    }
}
