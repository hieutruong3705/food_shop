package com.example.shop.domain;
import java.util.List;
/** One source for both admin action visibility and backend transition validation. */
public final class OrderTransitions {
    private OrderTransitions(){}
    public static List<String> allowed(Order order){
        if(order.isRefundRequired())return List.of();
        var states=switch(order.getStatus()){
            case "PENDING" -> List.of("CONFIRMED","CANCELLED");
            case "CONFIRMED" -> List.of("SHIPPING","CANCELLED");
            case "SHIPPING" -> List.of("COMPLETED");
            default -> List.<String>of();
        };
        return states.stream().filter(next->"CANCELLED".equals(next)?!"PAID".equals(order.getPaymentStatus()):!"VNPAY".equals(order.getPaymentMethod())||"PAID".equals(order.getPaymentStatus())).toList();
    }
}
