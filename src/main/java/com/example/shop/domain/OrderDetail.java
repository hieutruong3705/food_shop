package com.example.shop.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "order_details")
public class OrderDetail {
    @jakarta.persistence.ManyToOne @jakarta.persistence.JoinColumn(name="variant_id") private ProductVariant variant;
    @jakarta.persistence.Column(length=2000) private String variantLabel;
    public ProductVariant getVariant(){return variant;}
    public void setVariant(ProductVariant value){variant=value;}
    public String getVariantLabel(){return variantLabel;}
    public void setVariantLabel(String value){variantLabel=value;}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long quantity;
    private double price;

    private String productName;
    private String productSku;
    public String getProductName() { return productName == null ? product.getName() : productName; }
    public void setProductName(String value) { productName = value; }
    public String getProductSku() { return productSku; }
    public void setProductSku(String value) { productSku = value; }

    // ĐÃ XÓA: private String selectedColor;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // ĐÃ XÓA: getSelectedColor() và setSelectedColor()

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
