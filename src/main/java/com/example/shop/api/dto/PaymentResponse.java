package com.example.shop.api.dto;
public final class PaymentResponse {
    private PaymentResponse() {}
    /** Return-URL read-out. `amount` is VND, not the gateway's hundredths. Everything but `valid` and `message` is null when the signature fails. */
    public record ReturnView(boolean valid, Long orderId, String responseCode, String transactionStatus, Long amount,
        String bankCode, String transactionNo, String orderStatus, String paymentStatus, String paymentReference,
        boolean refundRequired, String message) {}
    /** `expiresAt` is an ISO-8601 instant, null unless the order is still an unpaid VNPay order. */
    public record StatusView(String orderStatus, String paymentStatus, String paymentReference, String expiresAt, boolean refundRequired) {}
}
