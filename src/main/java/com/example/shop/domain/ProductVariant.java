package com.example.shop.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity @Table(name="product_variants")
public class ProductVariant {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="product_id") private Product product;
    @Column(nullable=false,unique=true,length=80) private String sku;
    private double price;
    private long quantity;
    private boolean active=true;
    @Column(length=500) private String imageUrl;
    @ElementCollection
    @CollectionTable(name="variant_options",joinColumns=@JoinColumn(name="variant_id"))
    @MapKeyColumn(name="option_name",length=80) @Column(name="option_value",length=100)
    private Map<String,String> options=new LinkedHashMap<>();
    public Long getId(){return id;}
    public Product getProduct(){return product;}
    public void setProduct(Product value){product=value;}
    public String getSku(){return sku;}
    public void setSku(String value){sku=value;}
    public double getPrice(){return price;}
    public void setPrice(double value){price=value;}
    public long getQuantity(){return quantity;}
    public void setQuantity(long value){quantity=value;}
    public boolean isActive(){return active;}
    public void setActive(boolean value){active=value;}
    public String getImageUrl(){return imageUrl;}
    public void setImageUrl(String value){imageUrl=value;}
    public Map<String,String> getOptions(){return options;}
    public String getLabel(){return options.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+": "+e.getValue()).collect(java.util.stream.Collectors.joining(" · "));}
}
