package com.example.shop.api.controller;

import com.example.shop.repository.*;
import com.example.shop.domain.*;
import com.example.shop.api.service.AdminCatalogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController @RequestMapping("/api/v1") @Transactional(readOnly=true)
public class OffersApiController {
    private final PromotionRepository promotions; private final VoucherRepository vouchers; private final ProductRepository products;
    public OffersApiController(PromotionRepository p,VoucherRepository v,ProductRepository products){promotions=p;vouchers=v;this.products=products;}
    private List<String> scopeNames(ScopedOffer offer){return switch(offer.getScopeType()){
        case ALL -> List.of();case CATEGORY -> offer.getCategories().stream().map(Category::getName).toList();
        case BRAND -> offer.getBrands().stream().map(Brand::getName).toList();case PRODUCT -> offer.getProducts().stream().map(Product::getName).toList();
    };}
    @GetMapping("/offers") public Map<String,Object> offers(){
        var now=new Date();
        var activePromotions=promotions.findAll().stream().filter(p -> p.isActive()&&p.getStartDate()!=null&&p.getEndDate()!=null&&p.getStartDate().before(now)&&p.getEndDate().after(now)&&p.getDiscountRate()>0&&p.getDiscountRate()<=100)
            .filter(p -> p.getScopeType()==ScopeType.ALL||!scopeNames(p).isEmpty())
            .sorted(Comparator.comparingDouble(Promotion::getDiscountRate).reversed().thenComparing(Comparator.comparingLong(Promotion::getId).reversed()))
            .map(p -> AdminCatalogService.map("id",p.getId(),"name",p.getName(),"description",p.getDescription(),"discountRate",p.getDiscountRate(),"endDate",p.getEndDate(),"hasAvailableProducts",products.count(OfferScopeSpecification.visible(p))>0,"scopeType",p.getScopeType(),"scopeNames",scopeNames(p))).toList();
        var activeVouchers=vouchers.findAll().stream().filter(v -> v.isActive()&&v.getDiscount()>0&&v.getDiscount()<=100&&(v.getStartDate()==null||!now.before(v.getStartDate()))&&(v.getEndDate()==null||now.before(v.getEndDate()))&&(v.getUsageLimit()==null||v.getUsedCount()<v.getUsageLimit()))
            .filter(v -> v.isAll() || products.count(OfferScopeSpecification.visible(v))>0)
            .sorted(Comparator.comparingDouble(Voucher::getDiscount).reversed().thenComparing(Comparator.comparingLong(Voucher::getId).reversed()))
            .map(v -> AdminCatalogService.map("code",v.getCode(),"discount",v.getDiscount(),"minimumOrder",v.getMinimumOrder(),"maximumDiscount",v.getMaximumDiscount(),"endDate",v.getEndDate(),"all",v.isAll(),"scopeType",v.getScopeType(),"scopeNames",scopeNames(v),"categories",v.getCategories().stream().map(c->Map.of("id",c.getId(),"name",c.getName())).toList())).toList();
        return Map.of("promotions",activePromotions,"vouchers",activeVouchers);
    }
}
