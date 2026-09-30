package com.example.shop.api.service;
import com.example.shop.api.dto.admin.*;
import com.example.shop.api.dto.CommerceResponse;
import com.example.shop.api.dto.catalog.VariantResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.example.shop.api.service.AdminSupport.*;

@Service
@Transactional
public class BrandAdminService {
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final VoucherRepository vouchers;
    private final PromotionRepository promotions;
    public BrandAdminService(CategoryRepository categories, BrandRepository brands, VoucherRepository vouchers, PromotionRepository promotions) {
        this.categories = categories;
        this.brands = brands;
        this.vouchers = vouchers;
        this.promotions = promotions;
    }
    @Transactional(readOnly = true)
    public Page<?> list(int page,int size,String search,Map<String,String> filters) {
        var paging=page(page,size,search);
        String type="brands";
return brands.findAll(matching(search,type,filters,"name"),paging).map(b -> map("id",b.getId(),"name",b.getName(),"description",b.getDescription(),"logoUrl",b.getLogoUrl(),"productCount",b.getProductCount()));
    }
    @Transactional(readOnly = true)
    public Object detail(long id) {
        String type="brands";
var b=brands.findById(id).orElseThrow(() -> missing(type,id));return map("id",b.getId(),"name",b.getName(),"description",b.getDescription(),"logoUrl",b.getLogoUrl());
    }
    public Map<String,Object> save(Long id,BrandInput n,String currentEmail) {
        String type="brands";
var b=id==null?new Brand():brands.findById(id).orElseThrow(() -> missing(type,id));b.setName(n.name().trim());b.setDescription(optional(n.description()));b.setLogoUrl(url(optional(n.logoUrl())));return map("id",brands.save(b).getId());
    }
    public void delete(long id,String currentEmail) {
        String type="brands";
var b=brands.findById(id).orElseThrow(() -> missing(type,id));var refs=new ArrayList<String>();b.getProducts().stream().map(Product::getName).sorted().forEach(refs::add);refs.addAll(vouchers.codesByBrand(id));refs.addAll(promotions.namesByBrand(id));refs.addAll(categories.namesByBrand(id));if(!refs.isEmpty())throw new IllegalArgumentException("Thương hiệu đang được dùng bởi: "+String.join(", ",refs));brands.delete(b);
    }

}
