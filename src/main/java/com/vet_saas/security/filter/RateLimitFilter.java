package com.vet_saas.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastAccessTimes = new ConcurrentHashMap<>();
    private static final long BUCKET_MAX_AGE_MS = 300_000; // 5 minutos

    public RateLimitFilter() {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::evictStaleBuckets, 5, 5, TimeUnit.MINUTES);
    }

    private void evictStaleBuckets() {
        long now = System.currentTimeMillis();
        // Evict buckets older than BUCKET_MAX_AGE_MS
        Iterator<Map.Entry<String, Long>> it = lastAccessTimes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > BUCKET_MAX_AGE_MS) {
                it.remove();
                buckets.remove(entry.getKey());
            }
        }
        // Safety net: if still too many buckets, remove oldest entries
        if (buckets.size() > 1000) {
            lastAccessTimes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(buckets.size() - 800)
                .forEach(e -> {
                    buckets.remove(e.getKey());
                    lastAccessTimes.remove(e.getKey());
                });
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIdentifier(request);

        String rateLimitKey = clientIp + ":" + path;

        Bucket bucket = buckets.computeIfAbsent(rateLimitKey, this::createBucket);
        lastAccessTimes.put(rateLimitKey, System.currentTimeMillis());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitTimeSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Demasiadas solicitudes. Intenta de nuevo en " + waitTimeSeconds + " segundos.\"}");
        }
    }

    private Bucket createBucket(String key) {
        Bandwidth limit;
        if (key.contains("/auth/login") || key.contains("/auth/register")) {
            limit = Bandwidth.simple(5, Duration.ofMinutes(1));
        } else if (key.contains("/auth/forgot-password") || key.contains("/auth/reset-password")) {
            limit = Bandwidth.simple(3, Duration.ofMinutes(5));
        } else if (key.contains("/auth/sync")) {
            limit = Bandwidth.simple(10, Duration.ofMinutes(1));
        } else if (key.contains("/payments/webhook")) {
            limit = Bandwidth.simple(100, Duration.ofMinutes(1));
        } else if (key.contains("/payments/checkout")) {
            limit = Bandwidth.simple(10, Duration.ofMinutes(1));
        } else if (key.contains("/reclamos")) {
            limit = Bandwidth.simple(5, Duration.ofMinutes(1));
        } else if (key.contains("/newsletter/subscribe")) {
            limit = Bandwidth.simple(3, Duration.ofMinutes(1));
        } else if (key.contains("/referrals/apply")) {
            limit = Bandwidth.simple(10, Duration.ofMinutes(1));
        } else {
            limit = Bandwidth.simple(30, Duration.ofMinutes(1));
        }

        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/auth/")
                && !path.startsWith("/api/v1/payments/webhook")
                && !path.startsWith("/api/v1/payments/checkout")
                && !path.startsWith("/api/v1/reclamos")
                && !path.startsWith("/api/v1/newsletter")
                && !path.startsWith("/api/v1/referrals/apply");
    }
}
