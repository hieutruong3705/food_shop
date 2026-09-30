package com.example.shop.repository;

import com.example.shop.domain.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Promotion> {
    /** Promotions in effect at {@code now}: active, started, not yet ended, with a usable rate. */
    @org.springframework.data.jpa.repository.Query("select p from Promotion p where p.active = true and p.startDate < :now and p.endDate > :now and p.discountRate > 0 and p.discountRate <= 100")
    java.util.List<Promotion> findRunning(@org.springframework.data.repository.query.Param("now") java.util.Date now);
    @org.springframework.data.jpa.repository.Query("select p.id, c.id from Promotion p join p.categories c where p.id in :ids")
    java.util.List<Object[]> categoryLinks(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
    @org.springframework.data.jpa.repository.Query("select p.id, b.id from Promotion p join p.brands b where p.id in :ids")
    java.util.List<Object[]> brandLinks(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
    @org.springframework.data.jpa.repository.Query("select p.id, x.id from Promotion p join p.products x where p.id in :ids")
    java.util.List<Object[]> productLinks(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
    /** Names of the promotions whose scope lists the category; these rows block deleting it. */
    @org.springframework.data.jpa.repository.Query("select p.name from Promotion p join p.categories c where c.id = :id order by p.name")
    java.util.List<String> namesByCategory(@org.springframework.data.repository.query.Param("id") long id);
    @org.springframework.data.jpa.repository.Query("select p.name from Promotion p join p.brands b where b.id = :id order by p.name")
    java.util.List<String> namesByBrand(@org.springframework.data.repository.query.Param("id") long id);
}