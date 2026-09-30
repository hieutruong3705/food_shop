package com.example.shop.repository;

import com.example.shop.domain.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    interface SalesRank { Long getProductId(); Long getUnits(); }
    @org.springframework.data.jpa.repository.Query(value="""
        SELECT d.product_id AS productId, SUM(d.quantity) AS units
        FROM order_details d JOIN orders o ON o.id=d.order_id JOIN products p ON p.id=d.product_id
        WHERE p.category_id=:categoryId AND p.active=true AND p.technology_catalog=true
          AND o.status IN ('CONFIRMED','SHIPPING','COMPLETED')
          AND (o.payment_method='COD' OR o.payment_status='PAID')
        GROUP BY d.product_id ORDER BY SUM(d.quantity) DESC, d.product_id DESC
        """,nativeQuery=true)
    java.util.List<SalesRank> topSales(@org.springframework.data.repository.query.Param("categoryId") long categoryId,org.springframework.data.domain.Pageable limit);
    /** Best sellers of the whole shop; counted exactly like {@link #topSales}. */
    @org.springframework.data.jpa.repository.Query(value="""
        SELECT d.product_id AS productId, SUM(d.quantity) AS units
        FROM order_details d JOIN orders o ON o.id=d.order_id JOIN products p ON p.id=d.product_id
        WHERE p.active=true AND p.technology_catalog=true
          AND o.status IN ('CONFIRMED','SHIPPING','COMPLETED')
          AND (o.payment_method='COD' OR o.payment_status='PAID')
        GROUP BY d.product_id ORDER BY SUM(d.quantity) DESC, d.product_id DESC
        """,nativeQuery=true)
    java.util.List<SalesRank> topSalesOverall(org.springframework.data.domain.Pageable limit);
    @org.springframework.data.jpa.repository.Query("select (count(d)>0) from OrderDetail d where d.product.id=:id and d.variant is null and d.order.status not in (com.example.shop.domain.OrderStatus.COMPLETED,com.example.shop.domain.OrderStatus.CANCELLED)")
    boolean hasOpenSimpleOrders(@org.springframework.data.repository.query.Param("id") long id);

    boolean existsByOrderUserEmailAndProductIdAndOrderStatus(String email, long productId, com.example.shop.domain.OrderStatus status);
}
