package com.autoconsultancy.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter applied to authentication endpoints.
 *
 * <p>Limits: 5 requests per minute per IP address on {@code /api/auth/**}.
 * On exceeding the limit the filter returns HTTP 429 without reaching any service.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** Maximum number of auth attempts per minute per IP. */
    private static final int MAX_AUTH_ATTEMPTS = 5;

    /** Evict buckets idle for more than 10 minutes to avoid unbounded memory growth. */
    private static final long BUCKET_IDLE_TTL_MS = 10 * 60 * 1_000L;

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate-limit authentication endpoints
        if (!path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = getBucket(clientIp);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please wait a moment before trying again.\"}");
        }
    }

    private Bucket getBucket(String clientIp) {
        long now = System.currentTimeMillis();
        // Evict stale entries periodically (simple TTL cleanup on access)
        buckets.entrySet().removeIf(e -> now - e.getValue().lastAccess > BUCKET_IDLE_TTL_MS);

        BucketEntry entry = buckets.computeIfAbsent(clientIp, k -> new BucketEntry(createBucket()));
        entry.lastAccess = now;
        return entry.bucket;
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                MAX_AUTH_ATTEMPTS,
                Refill.intervally(MAX_AUTH_ATTEMPTS, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Respect X-Forwarded-For when behind a reverse proxy
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Wrapper that tracks last-access time for eviction. */
    private static class BucketEntry {
        final Bucket bucket;
        volatile long lastAccess;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccess = System.currentTimeMillis();
        }
    }
}
