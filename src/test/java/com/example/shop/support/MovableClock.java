package com.example.shop.support;

import com.example.shop.config.TimeConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A Clock whose instant a test can set or advance. Register it as a {@code @Primary} bean. */
public class MovableClock extends Clock {
    private volatile Instant now;

    public MovableClock(Instant start) {
        this.now = start;
    }

    public void set(Instant value) {
        now = value;
    }

    public void advance(Duration amount) {
        now = now.plus(amount);
    }

    @Override
    public ZoneId getZone() {
        return TimeConfig.SHOP_ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
