package com.example.shop.repository;

import com.example.shop.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Category> {

    List<Category> findByNameContainingIgnoreCase(String name);
    /** Names of the categories that list the brand (category_brands); these rows block deleting it. */
    @org.springframework.data.jpa.repository.Query("select c.name from Category c join c.brands b where b.id = :id order by c.name")
    List<String> namesByBrand(@org.springframework.data.repository.query.Param("id") long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Category c where c.id=:id")
    java.util.Optional<Category> lockById(@org.springframework.data.repository.query.Param("id") long id);
}
