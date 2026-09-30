package com.example.shop.api.dto;
import java.util.*;
import com.example.shop.domain.*;
public final class CommerceResponse {
    private CommerceResponse() {}
    public record CartLine(long id, long productId, String name, String image, long quantity, long stock, double price, boolean active, Long variantId, String variantLabel) {}
    public record CartView(List<CartLine> items, long count, double total) {}
    public record Quote(double subtotal,double shippingFee,double discountAmount,double totalPrice,String voucherCode) {}
    public record OrderLine(long id, long productId, String name, String sku, long quantity, double price, Long variantId, String variantLabel) {}
    public record OrderView(long id, String receiverName, String receiverAddress, String receiverPhone,
        String status, String paymentMethod, String paymentStatus, Date createdAt, double totalPrice,
        Double shippingFee, Double discountAmount, String voucherCode, List<OrderLine> items, boolean archived, String note, List<String> allowedStatuses) {}
    public record ReviewView(long id, int rating, String content, String author, long productId, String productName, Date createdAt, boolean editable) {}
    public static OrderView order(Order o) {
        return new OrderView(o.getId(),o.getReceiverName(),o.getReceiverAddress(),o.getReceiverPhone(),o.getStatus(),o.getPaymentMethod(),o.getPaymentStatus(),o.getCreatedAt(),o.getTotalPrice(),o.getShippingFee(),o.getDiscountAmount(),o.getVoucherCode(),o.getOrderDetails().stream().map(d -> new OrderLine(d.getId(),d.getProduct().getId(),d.getProductName(),d.getProductSku(),d.getQuantity(),d.getPrice(),d.getVariant()==null?null:d.getVariant().getId(),d.getVariantLabel())).toList(),o.isAdminArchived(),o.getNote(),OrderTransitions.allowed(o));
    }
    public static ReviewView review(Review r) { return review(r,null); }
    public static ReviewView review(Review r,String email) { return new ReviewView(r.getId(),r.getRating(),r.getContent(),r.getUser().getFullName(),r.getProduct().getId(),r.getProduct().getName(),r.getCreatedAt(),email!=null && email.equals(r.getUser().getEmail())); }
}
