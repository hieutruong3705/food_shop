package com.example.shop.domain;

import java.util.Locale;

/** Where an order is in its life. The API and the database both use the constant names. */
public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED;

    /** Reads the name sent by a client; an unknown name is a 400, not a 500. */
    public static OrderStatus parse(String value) {
        try {
            return valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ");
        }
    }
}
