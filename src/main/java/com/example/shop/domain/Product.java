package com.example.shop.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "products")
public class Product implements Serializable {
    @jakarta.persistence.Column(nullable=false)
    private boolean technologyCatalog=true;
    public boolean isTechnologyCatalog(){return technologyCatalog;}
    public void setTechnologyCatalog(boolean value){technologyCatalog=value;}


    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true) private String sku;
    @Column(unique = true) private String slug;
    private Integer warrantyMonths;
    public String getSku() { return sku; }
    public void setSku(String value) { sku = value; }
    public String getSlug() { return slug; }
    public void setSlug(String value) { slug = value; }
    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer value) { warrantyMonths = value; }

    private double price;
    @OneToMany(mappedBy="product",cascade=CascadeType.ALL)
    @OrderBy("id ASC")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<ProductVariant> variants=new ArrayList<>();
    public List<ProductVariant> getVariants(){return variants;}
    public boolean hasVariants(){return !variants.isEmpty();}

    // Thêm trường active cho Soft Delete (Mặc định là true - Đang kinh doanh)
    @Column(columnDefinition = "boolean default true")
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC, id ASC")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<ProductImage> images = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String detailDesc;

    @Column(columnDefinition = "TEXT")
    private String shortDesc;

    private long quantity;
    private long sold;
    private String factory;
    private String target;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<ProductSpec> specs = new ArrayList<>();

    // --- PHẦN MỚI THÊM: LIÊN KẾT VỚI PROMOTION (KHUYẾN MÃI) ---
    @ManyToMany(mappedBy = "products", fetch = FetchType.LAZY)
    private List<Promotion> promotions = new ArrayList<>();

    // Các trường tính toán (Formula)
    @Formula("(SELECT COALESCE(AVG(r.rating), 0) FROM reviews r WHERE r.product_id = id)")
    private double averageRating;

    @Formula("(SELECT COUNT(r.id) FROM reviews r WHERE r.product_id = id)")
    private int reviewCount;

    // --- LIÊN KẾT VỚI BẢNG BRAND (HÃNG SẢN XUẤT) ---
    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;
    private String origin;

    public Product() {
    }

    // The sale price is not stored or computed here: PromotionPricing applies the running promotions.

    // --- GETTER & SETTER ---

    public List<Promotion> getPromotions() {
        return promotions;
    }

    public void setPromotions(List<Promotion> promotions) {
        this.promotions = promotions;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public String getDetailDesc() {
        return detailDesc;
    }

    public void setDetailDesc(String detailDesc) {
        this.detailDesc = detailDesc;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public long getSold() {
        return sold;
    }

    public void setSold(long sold) {
        this.sold = sold;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<ProductSpec> getSpecs() {
        return specs;
    }

    public void setSpecs(List<ProductSpec> specs) {
        this.specs = specs;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    @Override
    public String toString() {
        return "Product [id=" + id + ", name=" + name + ", price=" + price + "]";
    }

    public String getImage() {
        if (this.images != null && !this.images.isEmpty()) {
            return this.images.get(0).getImageUrl();
        }
        return "";
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

}
