package com.soodfolio.api.config;

import com.soodfolio.api.service.PublicRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class PublicEndpointRateLimitInterceptor implements HandlerInterceptor {

    private final ObjectProvider<PublicRateLimiter> rateLimiterProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${app.rate-limit.public-enabled:true}")
    private boolean rateLimitEnabled;

    public PublicEndpointRateLimitInterceptor(ObjectProvider<PublicRateLimiter> rateLimiterProvider) {
        this.rateLimiterProvider = rateLimiterProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitEnabled) {
            return true;
        }

        PublicRateLimiter rateLimiter = rateLimiterProvider.getIfAvailable();
        if (rateLimiter == null) {
            return true;
        }

        // Only enforce on POST requests
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        String path = resolveLookupPath(request);
        String uri = request.getRequestURI();
        String clientIp = extractClientIp(request);

        if (matchesPattern("/auth/forgot-password", path, uri)) {
            rateLimiter.checkRateLimit("forgot-password", clientIp, 5);
        } else if (matchesPattern("/auth/login", path, uri)) {
            rateLimiter.checkRateLimit("login", clientIp, 15);
        } else if (matchesPattern("/portfolios/*/views", path, uri)) {
            rateLimiter.checkRateLimit("views", clientIp, 30);
        } else if (matchesPattern("/public/links/*/telemetry", path, uri)) {
            rateLimiter.checkRateLimit("telemetry", clientIp, 60);
        }

        return true;
    }

    private boolean matchesPattern(String pattern, String path, String uri) {
        return pathMatcher.match(pattern, path)
                || pathMatcher.match("/**" + pattern, uri)
                || pathMatcher.match(pattern, uri);
    }

    public String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                return parts[0].trim();
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : "127.0.0.1";
    }

    private String resolveLookupPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
        }
        return path;
    }
}
