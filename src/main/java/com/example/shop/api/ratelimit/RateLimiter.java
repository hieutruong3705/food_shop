package com.example.shop.api.ratelimit;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window counter kept in memory: at most {@code limit} hits per key inside {@code window}.
 * State is per instance, which is enough for a single-node deployment. Idle keys are dropped
 * during normal use so the map cannot grow without bound.
 */
@Component
public class RateLimiter {
    private static final long SWEEP_EVERY_MILLIS = 60_000;

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private volatile long nextSweep;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records a hit. Returns 0 when it is allowed, otherwise the whole seconds until it would be. */
    public long acquire(String key, int limit, Duration window) {
        long now = clock.millis();
        sweep(now);
        long[] wait = {0};
        windows.compute(key, (k, current) -> {
            Window w = current == null ? new Window() : current;
            wait[0] = w.hit(now, limit, window.toMillis());
            return w;
        });
        return wait[0];
    }

    int trackedKeys() {
        return windows.size();
    }

    private void sweep(long now) {
        if (now < nextSweep) return;
        nextSweep = now + SWEEP_EVERY_MILLIS;
        for (String key : windows.keySet()) {
            windows.computeIfPresent(key, (k, w) -> w.idleAt(now) ? null : w);
        }
    }

    private static final class Window {
        private final ArrayDeque<Long> hits = new ArrayDeque<>();
        private long windowMillis;

        long hit(long now, int limit, long windowMillis) {
            this.windowMillis = windowMillis;
            while (!hits.isEmpty() && now - hits.peekFirst() >= windowMillis) hits.removeFirst();
            if (hits.size() >= limit) {
                return Math.max(1, (hits.peekFirst() + windowMillis - now + 999) / 1000);
            }
            hits.addLast(now);
            return 0;
        }

        boolean idleAt(long now) {
            return hits.isEmpty() || now - hits.peekLast() >= windowMillis;
        }
    }
}
