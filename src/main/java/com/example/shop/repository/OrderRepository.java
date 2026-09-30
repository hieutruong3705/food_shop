package com.example.shop.repository;

import com.example.shop.domain.Order;
import com.example.shop.domain.OrderStatus;
import com.example.shop.domain.PaymentStatus;
import com.example.shop.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Order> {
    @Query("select o from Order o where o.status = com.example.shop.domain.OrderStatus.COMPLETED and coalesce(o.completedAt,o.createdAt) >= :start and coalesce(o.completedAt,o.createdAt) < :end")
    List<Order> findCompletedBetween(@Param("start") java.util.Date start, @Param("end") java.util.Date end);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    List<Order> findByCreatedAtBetween(java.util.Date start, java.util.Date end);

    List<Order> findByPaymentMethodAndStatusAndPaymentStatusAndCreatedAtBefore(String method, OrderStatus status, PaymentStatus paymentStatus, java.util.Date before);

    java.util.Optional<Order> findByCheckoutKey(String checkoutKey);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    java.util.Optional<Order> lockById(@Param("id") long id);

    List<Order> findByUserOrderByIdDesc(User user);

    long count();
}
