package com.example.shop.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Duration;
import java.util.Map;

/**
 * Per-client-address limits on the endpoints that cost money or send e-mail. The address is
 * {@code getRemoteAddr()}: X-Forwarded-For is honoured only when the server is configured to
 * trust a proxy (server.forward-headers-strategy), never straight from a request header.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    /** Paths the interceptor is registered for. */
    public static final String[] PATHS = {
        "/api/chat", "/api/v1/chat", "/api/v1/auth/login",
        "/api/v1/auth/register", "/api/v1/auth/forgot-password", "/api/v1/auth/resend-verification"};

    /** Aliases of one endpoint share a bucket, otherwise alternating paths would double the limit. */
    private record Rule(String bucket, int limit, Duration window) {}

    private static final Rule CHAT = new Rule("chat", 12, Duration.ofMinutes(1));
    private static final Rule LOGIN = new Rule("login", 10, Duration.ofMinutes(1));
    private static final Map<String, Rule> RULES = Map.of(
        "/api/chat", CHAT, "/api/v1/chat", CHAT,
        "/api/v1/auth/login", LOGIN,
        "/api/v1/auth/register", mail("register"),
        "/api/v1/auth/forgot-password", mail("forgot-password"),
        "/api/v1/auth/resend-verification", mail("resend-verification"));

    /** Endpoints that send e-mail: 5 requests every 10 minutes, counted separately for each endpoint. */
    private static Rule mail(String bucket) {
        return new Rule(bucket, 5, Duration.ofMinutes(10));
    }

    private final RateLimiter limiter;
    private final boolean enabled;

    public RateLimitInterceptor(RateLimiter limiter, @Value("${shop.rate-limit.enabled:true}") boolean enabled) {
        this.limiter = limiter;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || !"POST".equals(request.getMethod())) return true;
        String path = request.getRequestURI();
        Rule rule = RULES.get(path);
        if (rule == null) return true;
        long wait = limiter.acquire(rule.bucket() + "|" + request.getRemoteAddr(), rule.limit(), rule.window());
        if (wait > 0) {
            throw new RateLimitExceededException("Bạn thao tác quá nhanh. Vui lòng thử lại sau " + describe(wait) + ".", wait);
        }
        return true;
    }

    /** Seconds under two minutes, whole minutes above. */
    public static String describe(long seconds) {
        return seconds >= 120 ? (seconds + 59) / 60 + " phút" : seconds + " giây";
    }
}
