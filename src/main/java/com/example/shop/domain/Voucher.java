package com.example.shop.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "vouchers")
public class Voucher implements Serializable,ScopedOffer {
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @jakarta.persistence.Column(nullable=false,length=20) private ScopeType scopeType=ScopeType.CATEGORY;
    public ScopeType getScopeType(){return scopeType;}
    public void setScopeType(ScopeType type){scopeType=type;isAll=type==ScopeType.ALL;}
    @ManyToMany
    @JoinTable(name="voucher_brand",joinColumns=@JoinColumn(name="voucher_id"),inverseJoinColumns=@JoinColumn(name="brand_id"))
    private List<Brand> brands=new ArrayList<>();
    @ManyToMany
    @JoinTable(name="voucher_product",joinColumns=@JoinColumn(name="voucher_id"),inverseJoinColumns=@JoinColumn(name="product_id"))
    private List<Product> products=new ArrayList<>();
    public List<Brand> getBrands(){return brands;}public void setBrands(List<Brand> value){brands=value;}
    public List<Product> getProducts(){return products;}public void setProducts(List<Product> value){products=value;}
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String code;
    private double discount;
    private boolean isAll;
    @org.hibernate.annotations.ColumnDefault("true")
    @jakarta.persistence.Column(nullable=false) private boolean active = true;
    private java.util.Date startDate;
    private java.util.Date endDate;
    @org.hibernate.annotations.ColumnDefault("0")
    @jakarta.persistence.Column(nullable=false) private double minimumOrder;
    private Double maximumDiscount;
    private Long usageLimit;
    @org.hibernate.annotations.ColumnDefault("0")
    @jakarta.persistence.Column(nullable=false) private long usedCount;

    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public java.util.Date getStartDate() { return startDate; }
    public void setStartDate(java.util.Date value) { startDate = value; }
    public java.util.Date getEndDate() { return endDate; }
    public void setEndDate(java.util.Date value) { endDate = value; }
    public double getMinimumOrder() { return minimumOrder; }
    public void setMinimumOrder(double value) { minimumOrder = value; }
    public Double getMaximumDiscount() { return maximumDiscount; }
    public void setMaximumDiscount(Double value) { maximumDiscount = value; }
    public Long getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Long value) { usageLimit = value; }
    public long getUsedCount() { return usedCount; }
    public void setUsedCount(long value) { usedCount = value; }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "voucher_category", joinColumns = @JoinColumn(name = "voucher_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<Category> categories = new ArrayList<>();

    public Voucher() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public boolean isAll() {
        return scopeType==ScopeType.ALL;
    }

    public void setAll(boolean isAll) {
        this.isAll = isAll;
        this.scopeType=isAll?ScopeType.ALL:ScopeType.CATEGORY;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }
}
