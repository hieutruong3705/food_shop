package com.example.shop.domain;
import java.util.List;
/** One source for both admin action visibility and backend transition validation. */
public final class OrderTransitions {
    private OrderTransitions(){}
    public static List<OrderStatus> allowed(Order order){
        if(order.isRefundRequired())return List.of();
        var states=switch(order.getStatus()){
            case PENDING -> List.of(OrderStatus.CONFIRMED,OrderStatus.CANCELLED);
            case CONFIRMED -> List.of(OrderStatus.SHIPPING,OrderStatus.CANCELLED);
            case SHIPPING -> List.of(OrderStatus.COMPLETED);
            default -> List.<OrderStatus>of();
        };
        boolean paid=order.getPaymentStatus()==PaymentStatus.PAID;
        return states.stream().filter(next->next==OrderStatus.CANCELLED?!paid:!"VNPAY".equals(order.getPaymentMethod())||paid).toList();
    }
}
