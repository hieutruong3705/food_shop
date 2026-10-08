package com.example.shop.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "banners")
public class Banner {
    @jakarta.persistence.Column(nullable=false)
    private boolean technologyCatalog=true;
    public boolean isTechnologyCatalog(){return technologyCatalog;}
    public void setTechnologyCatalog(boolean value){technologyCatalog=value;}

    @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable=false,length=20) private BannerPlacement placement=BannerPlacement.HOME_HERO;
    public BannerPlacement getPlacement(){return placement;}
    public void setPlacement(BannerPlacement value){placement=value==null?BannerPlacement.HOME_HERO:value;}


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    private String imageUrl;

    private String link;

    @Column(columnDefinition = "boolean default true")
    private boolean active = true;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}