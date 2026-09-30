package com.example.shop.domain;

/**
 * Where an order's payment is. {@code REFUND_REQUIRED} means money arrived after the order was
 * cancelled or expired and someone must pay it back by hand.
 */
public enum PaymentStatus {
    UNPAID, PAID, FAILED, EXPIRED, REFUND_REQUIRED
}
