package com.example.shop.api.ratelimit;

import com.example.shop.support.MovableClock;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTests {
    private final MovableClock clock = new MovableClock(Instant.parse("2026-10-01T00:00:00Z"));
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void allowsTheLimitThenReportsHowLongToWait() {
        for (int i = 0; i < 3; i++) assertThat(limiter.acquire("k", 3, Duration.ofMinutes(1))).isZero();
        clock.advance(Duration.ofSeconds(20));
        assertThat(limiter.acquire("k", 3, Duration.ofMinutes(1))).isEqualTo(40);
        // A rejected attempt is not recorded, so it does not extend the wait.
        clock.advance(Duration.ofSeconds(39));
        assertThat(limiter.acquire("k", 3, Duration.ofMinutes(1))).isEqualTo(1);
    }

    @Test
    void theWindowSlidesAndKeysAreIndependent() {
        limiter.acquire("a", 1, Duration.ofSeconds(10));
        assertThat(limiter.acquire("a", 1, Duration.ofSeconds(10))).isPositive();
        assertThat(limiter.acquire("b", 1, Duration.ofSeconds(10))).isZero();
        clock.advance(Duration.ofSeconds(10));
        assertThat(limiter.acquire("a", 1, Duration.ofSeconds(10))).isZero();
    }

    @Test
    void idleKeysAreDroppedSoTheMapDoesNotGrowForever() {
        for (int i = 0; i < 200; i++) limiter.acquire("ip-" + i, 5, Duration.ofSeconds(30));
        assertThat(limiter.trackedKeys()).isEqualTo(200);
        clock.advance(Duration.ofMinutes(2));
        limiter.acquire("fresh", 5, Duration.ofSeconds(30));
        assertThat(limiter.trackedKeys()).isEqualTo(1);
    }

    @Test
    void fiveFailuresLockAnAddressForFiveMinutesAndSuccessClearsTheCount() {
        var attempts = new LoginAttempts(clock);
        for (int i = 0; i < 4; i++) attempts.failed("Buyer@Example.com");
        assertThat(attempts.lockedSeconds("buyer@example.com")).isZero();
        attempts.succeeded("buyer@example.com");
        for (int i = 0; i < 4; i++) attempts.failed("buyer@example.com");
        assertThat(attempts.lockedSeconds("buyer@example.com")).isZero();

        attempts.failed("buyer@example.com");
        assertThat(attempts.lockedSeconds("BUYER@example.com")).isEqualTo(300);
        clock.advance(Duration.ofMinutes(5));
        assertThat(attempts.lockedSeconds("buyer@example.com")).isZero();
        // The count restarts after a lock: four more failures are still allowed.
        for (int i = 0; i < 4; i++) attempts.failed("buyer@example.com");
        assertThat(attempts.lockedSeconds("buyer@example.com")).isZero();
    }

    @Test
    void oldFailuresAreForgotten() {
        var attempts = new LoginAttempts(clock);
        for (int i = 0; i < 4; i++) attempts.failed("slow@example.com");
        clock.advance(Duration.ofMinutes(16));
        for (int i = 0; i < 4; i++) attempts.failed("slow@example.com");
        assertThat(attempts.lockedSeconds("slow@example.com")).isZero();
    }
}
