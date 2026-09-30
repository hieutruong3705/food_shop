package com.example.shop.repository;

import com.example.shop.domain.Product;
import com.example.shop.domain.dto.TopProductDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> lockById(@org.springframework.data.repository.query.Param("id") long id);
    boolean existsBySku(String sku);
    /** SKUs are unique regardless of case, whatever the database collation says. */
    java.util.List<Product> findAllBySkuIgnoreCase(String sku);

    List<Product> findByNameContainingIgnoreCase(String name);

    long count();

    /** How many products a shopper can buy in each of the given categories, as {categoryId, count} rows. */
    @Query("select p.category.id, count(p) from Product p where p.active = true and p.technologyCatalog = true and p.category.id in :ids group by p.category.id")
    List<Object[]> sellableCountsByCategory(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    /** The same by brand, as {brandId, count} rows. */
    @Query("select p.brand.id, count(p) from Product p where p.active = true and p.technologyCatalog = true and p.brand.id in :ids group by p.brand.id")
    List<Object[]> sellableCountsByBrand(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    // === ĐÃ FIX LỖI NHÂN BẢN DỮ LIỆU (CARTESIAN PRODUCT) ===
    // Dùng Subquery để lấy ảnh thay vì LEFT JOIN để tránh quantity bị nhân lên theo
    // số lượng ảnh
    @Query("SELECT new com.example.shop.domain.dto.TopProductDTO(" +
            "p.id, " +
            "p.name, " +
            "(SELECT MIN(img.imageUrl) FROM ProductImage img WHERE img.product.id = p.id), " +
            "SUM(od.quantity), " +
            "SUM(od.price * od.quantity), " +
            "p.active) " +
            "FROM OrderDetail od " +
            "JOIN od.product p " +
            "JOIN od.order o " +
            "WHERE o.status <> com.example.shop.domain.OrderStatus.CANCELLED " + // Loại bỏ các đơn hàng đã bị Hủy
            "GROUP BY p.id, p.name, p.active " +
            "ORDER BY SUM(od.quantity) DESC")
    List<TopProductDTO> findBestSellingProducts(Pageable pageable);

    // Thêm hàm đếm sản phẩm có số lượng nhỏ hơn hoặc bằng một mức nào đó
    long countByQuantityLessThanEqual(long quantity);
}
