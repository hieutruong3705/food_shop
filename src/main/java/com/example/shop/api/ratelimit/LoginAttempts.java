package com.example.shop.api.ratelimit;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks an e-mail address for five minutes after five consecutive failed logins. Failures older
 * than fifteen minutes are forgotten, so occasional typos never add up. The lock applies whether
 * or not the address has an account, so it reveals nothing about which addresses exist.
 */
@Component
public class LoginAttempts {
    static final int MAX_FAILURES = 5;
    static final Duration LOCK = Duration.ofMinutes(5);
    static final Duration FORGET_AFTER = Duration.ofMinutes(15);
    private static final int MAX_TRACKED = 50_000;

    private static final class State {
        int failures;
        long lastFailure;
        long lockedUntil;
    }

    private final Clock clock;
    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    public LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    /** Whole seconds the address stays locked, or 0 when a login may be attempted. */
    public long lockedSeconds(String email) {
        long now = clock.millis();
        State state = states.get(key(email));
        if (state == null) return 0;
        synchronized (state) {
            return state.lockedUntil > now ? (state.lockedUntil - now + 999) / 1000 : 0;
        }
    }

    public void failed(String email) {
        long now = clock.millis();
        if (states.size() >= MAX_TRACKED) sweep(now);
        State state = states.computeIfAbsent(key(email), k -> new State());
        synchronized (state) {
            if (now - state.lastFailure >= FORGET_AFTER.toMillis() && state.lockedUntil <= now) state.failures = 0;
            state.failures++;
            state.lastFailure = now;
            if (state.failures >= MAX_FAILURES) {
                state.lockedUntil = now + LOCK.toMillis();
                state.failures = 0;
            }
        }
    }

    public void succeeded(String email) {
        states.remove(key(email));
    }

    private void sweep(long now) {
        states.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().lockedUntil <= now && now - e.getValue().lastFailure >= FORGET_AFTER.toMillis();
            }
        });
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
