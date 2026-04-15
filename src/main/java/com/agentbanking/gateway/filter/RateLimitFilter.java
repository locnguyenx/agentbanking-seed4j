package com.agentbanking.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of("""
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end
            return current
            """, Long.class);

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/", "/api/onboarding/register", "/management/",
            "/swagger-ui", "/v3/api-docs"
    );

    private final boolean rateLimitEnabled;
    private final int defaultRequestsPerMinute;
    private final int perIpRequestsPerMinute;
    private final RedisTemplate<String, ?> redisTemplate;

    public RateLimitFilter(
            @Value("${agentbanking.gateway.rate-limit.enabled:true}") boolean rateLimitEnabled,
            @Value("${agentbanking.gateway.rate-limit.default-requests-per-minute:60}") int defaultRequestsPerMinute,
            @Value("${agentbanking.gateway.rate-limit.per-ip-requests-per-minute:30}") int perIpRequestsPerMinute,
            RedisTemplate<String, ?> redisTemplate) {
        this.rateLimitEnabled = rateLimitEnabled;
        this.defaultRequestsPerMinute = defaultRequestsPerMinute;
        this.perIpRequestsPerMinute = perIpRequestsPerMinute;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!rateLimitEnabled || isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        int limit = getRateLimit(clientIp);

        if (isRateLimitExceeded(clientIp, limit)) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            sendRateLimitExceeded(response, limit);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitExceeded(String clientIp, int limit) {
        try {
            String key = RATE_LIMIT_KEY_PREFIX + clientIp;
            Long currentCount = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(limit),
                    String.valueOf(Duration.ofMinutes(1).toSeconds())
            );
            return currentCount != null && currentCount > limit;
        } catch (Exception e) {
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
            return false;
        }
    }

    private int getRateLimit(String clientIp) {
        return perIpRequestsPerMinute > 0 ? perIpRequestsPerMinute : defaultRequestsPerMinute;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendRateLimitExceeded(HttpServletResponse response, int limit) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("Retry-After", "60");
        response.getWriter().write(
                "{\"status\":\"FAILED\",\"error\":{\"code\":\"ERR_RATE_LIMIT\",\"message\":\"Rate limit exceeded\",\"action_code\":\"RETRY\",\"trace_id\":\"" +
                java.util.UUID.randomUUID() + "\"}}");
    }
}