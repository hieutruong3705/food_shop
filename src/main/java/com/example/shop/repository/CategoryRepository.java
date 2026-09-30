package com.example.shop.repository;

import com.example.shop.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Category> {

    List<Category> findByNameContainingIgnoreCase(String name);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Category c where c.id=:id")
    java.util.Optional<Category> lockById(@org.springframework.data.repository.query.Param("id") long id);
}
