package com.example.shop.domain;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "promotions")
public class Promotion implements Serializable,ScopedOffer {
    @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable=false,length=20) private ScopeType scopeType=ScopeType.PRODUCT;
    public ScopeType getScopeType(){return scopeType;}
    public void setScopeType(ScopeType type){scopeType=type;}
    @ManyToMany
    @JoinTable(name="promotion_category",joinColumns=@JoinColumn(name="promotion_id"),inverseJoinColumns=@JoinColumn(name="category_id"))
    private List<Category> categories=new java.util.ArrayList<>();
    public List<Category> getCategories(){return categories;}
    public void setCategories(List<Category> value){categories=value;}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;
    @Column(columnDefinition="TEXT") private String description;

    private double discountRate;

    // SỬA: Định dạng 'yyyy-MM-ddTHH:mm' để nhận được cả giờ phút từ form
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date endDate;

    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_promotion", joinColumns = @JoinColumn(name = "promotion_id"), inverseJoinColumns = @JoinColumn(name = "product_id"))
    private List<Product> products=new java.util.ArrayList<>();

    public Promotion() {
    }

    // --- LOGIC TRẠNG THÁI (Tính theo Giờ & Phút) ---
    public boolean isExpired() {
        return this.endDate != null && new Date().after(this.endDate);
    }

    public boolean isUpcoming() {
        return this.startDate != null && new Date().before(this.startDate);
    }

    // --- Getter & Setter ---
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(double discountRate) {
        this.discountRate = discountRate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    // Thêm vào class Promotion
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "brand_promotion", joinColumns = @JoinColumn(name = "promotion_id"), inverseJoinColumns = @JoinColumn(name = "brand_id"))
    private List<Brand> brands=new java.util.ArrayList<>();

    public List<Brand> getBrands() {
        return brands;
    }

    public void setBrands(List<Brand> brands) {
        this.brands = brands;
    }

}
