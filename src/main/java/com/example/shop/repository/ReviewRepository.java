package com.example.shop.repository;

import com.example.shop.domain.Product;
import com.example.shop.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.example.shop.domain.User;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Review> {

    List<Review> findByContentContainingOrUserFullNameContainingOrProductNameContaining(String content, String userName,
            String productName);

    List<Review> findByProduct(Product product);

    boolean existsByUserAndProduct(User user, Product product);
    boolean existsByUserEmailAndProductId(String email,long productId);
    interface Summary {long getCount();Double getAverage();}
    @Query("select count(r) as count, coalesce(avg(r.rating),0.0) as average from Review r where r.product.id=:productId")
    Summary summarize(@Param("productId") long productId);

    @Query("SELECT r FROM Review r WHERE " +
            "(:rating IS NULL OR r.rating = :rating) AND " +
            "(:keyword IS NULL OR :keyword = '' OR r.product.name LIKE %:keyword% OR r.user.fullName LIKE %:keyword% OR r.user.email LIKE %:keyword%) "
            +
            "ORDER BY r.id DESC")
    List<Review> searchReviews(@Param("keyword") String keyword, @Param("rating") Integer rating);
}
