package com.example.shop.api.dto;
import jakarta.validation.constraints.*;
public final class CommerceRequests {
    private CommerceRequests() {}
    public record AddItem(@Positive long productId, @Min(1) @Max(100) long quantity, @Positive Long variantId) {
        public AddItem(long productId,long quantity){this(productId,quantity,null);}
    }
    public record Quantity(@Min(1) @Max(100) long quantity) {}
    public record Checkout(@NotBlank @Size(max=100) String receiverName,
        @NotBlank @Size(max=500) String receiverAddress,
        @NotBlank @Pattern(regexp="[+0-9 ()-]{9,20}") String receiverPhone,
        @Size(max=50) String voucherCode, @NotBlank @Pattern(regexp="COD|VNPAY") String paymentMethod,
        @Size(max=100) java.util.List<@NotNull @Positive Long> cartItemIds, @Size(max=1000) String note) {
        public Checkout(String name,String address,String phone,String voucher,String payment){this(name,address,phone,voucher,payment,null,null);}
        public Checkout(String name,String address,String phone,String voucher,String payment,java.util.List<Long> items){this(name,address,phone,voucher,payment,items,null);}
    }
    public record Status(@NotBlank String status) {}
    public record Profile(@NotBlank @Size(max=100) String fullName, @Size(max=20) String phone, @Size(max=500) String address) {}
    public record Password(@NotBlank String currentPassword, @NotBlank @Size(min=8,max=72) String password) {}
    public record ReviewInput(@Min(1) @Max(5) int rating, @NotBlank @Size(max=2000) String content) {}
}
