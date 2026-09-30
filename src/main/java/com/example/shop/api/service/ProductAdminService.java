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
public class ProductAdminService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final VariantService variantService;
    private final SkuService skuService;
    private final PromotionPricing pricing;
    public ProductAdminService(ProductRepository products, CategoryRepository categories, BrandRepository brands, VariantService variantService, SkuService skuService, PromotionPricing pricing) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.variantService = variantService;
        this.skuService = skuService;
        this.pricing = pricing;
    }
    @Transactional(readOnly = true)
    public Page<?> list(int page,int size,String search,Map<String,String> filters) {
        var paging=page(page,size,search);
        String type="products";
return products.findAll(AdminSupport.<Product>matching(search,type,filters,"name","sku").and((root,q,cb) -> {
                if(q.getResultType()!=Long.class && q.getResultType()!=long.class)
                    q.orderBy(cb.asc(cb.<Integer>selectCase().when(cb.gt(root.get("quantity"),0),0).otherwise(1)),cb.desc(root.get("id")));
                return cb.conjunction();
            }),PageRequest.of(page,size)).map(this::product);
    }
    @Transactional(readOnly = true)
    public Object detail(long id) {
        String type="products";
return product(products.findById(id).orElseThrow(() -> missing(type,id)));
    }
    public Map<String,Object> product(Product p) { var sale=pricing.current().sale(p); return map("id",p.getId(),"name",p.getName(),"sku",p.getSku(),"slug",p.getSlug(),"price",p.getPrice(),"quantity",p.getQuantity(),"active",p.isActive(),"technologyCatalog",p.isTechnologyCatalog(),"shortDesc",p.getShortDesc(),"detailDesc",p.getDetailDesc(),"warrantyMonths",p.getWarrantyMonths(),"categoryId",p.getCategory()==null?null:p.getCategory().getId(),"brandId",p.getBrand()==null?null:p.getBrand().getId(),"categoryName",p.getCategory()==null?null:p.getCategory().getName(),"brandName",p.getBrand()==null?null:p.getBrand().getName(),"discountRate",sale==null?0:sale.rate(),"promotionName",sale==null?null:sale.name(),"images",p.getImages().stream().map(ProductImage::getImageUrl).toList(),"variants",p.getVariants().stream().map(v -> VariantResponse.from(v,pricing.current())).toList(),"specs",p.getSpecs().stream().map(s -> map("name",s.getSpecName(),"value",s.getSpecValue())).toList()); }
    public Map<String,Object> selling(long id,boolean active) {
        var product=products.lockById(id).orElseThrow(() -> missing("products",id));
        product.setActive(active);
        return map("id",product.getId(),"active",product.isActive(),"quantity",product.getQuantity());
    }
    public Map<String,Object> save(Long id,ProductInput n,String currentEmail) {
        String type="products";

                Product p=id==null?new Product():products.lockById(id).orElseThrow(() -> missing(type,id));
                p.setTechnologyCatalog(enabled(n.technologyCatalog()));p.setName(n.name().trim());skuService.checkProduct(n.sku().trim(),p.getId());p.setSku(n.sku().trim());p.setSlug(n.slug().trim());
                if(!p.getSlug().matches("[a-z0-9-]+")) throw new IllegalArgumentException("Slug chỉ gồm chữ thường, số và dấu gạch ngang");
                p.setPrice(n.price());p.setQuantity(n.quantity());p.setActive(enabled(n.active()));
                p.setShortDesc(optional(n.shortDesc()));p.setDetailDesc(optional(n.detailDesc()));p.setWarrantyMonths(n.warrantyMonths()==null?0:n.warrantyMonths());
                p.setCategory(categories.lockById(n.categoryId()).orElseThrow(() -> new IllegalArgumentException("Chọn danh mục hợp lệ")));
                p.setBrand(brands.findById(n.brandId()).orElseThrow(() -> new IllegalArgumentException("Chọn thương hiệu hợp lệ")));
                if(p.getCategory().getBrands().stream().noneMatch(b->b.getId()==p.getBrand().getId()))throw new IllegalArgumentException("Thương hiệu không thuộc danh mục đã chọn. Hãy gán thương hiệu trong quản trị Danh mục.");
                p.getImages().clear();if(AdminSupport.list(n.images()).size()>12)throw new IllegalArgumentException("Tối đa 12 ảnh");
                for(String value:AdminSupport.list(n.images())) { var image=new ProductImage();image.setProduct(p);image.setSortOrder(p.getImages().size());image.setImageUrl(url(value));p.getImages().add(image); }
                p.getSpecs().clear();if(AdminSupport.list(n.specs()).size()>40)throw new IllegalArgumentException("Tối đa 40 thông số");
                for(SpecInput value:AdminSupport.list(n.specs())) { var spec=new ProductSpec();spec.setProduct(p);spec.setSpecName(value.name().trim());spec.setSpecValue(value.value().trim());p.getSpecs().add(spec); }
                if(n.variants()!=null)variantService.save(p,n.variants());
                else if(p.hasVariants())throw new IllegalArgumentException("Gửi danh sách phiên bản khi cập nhật sản phẩm có tùy chọn");
                return map("id",products.save(p).getId());
    }
    public void delete(long id,String currentEmail) {
        String type="products";
selling(id,false);
    }

}
