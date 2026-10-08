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
public class CategoryAdminService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final VoucherRepository vouchers;
    private final PromotionRepository promotions;
    public CategoryAdminService(ProductRepository products, CategoryRepository categories, BrandRepository brands, VoucherRepository vouchers, PromotionRepository promotions) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.vouchers = vouchers;
        this.promotions = promotions;
    }
    @Transactional(readOnly = true)
    public Page<?> list(int page,int size,String search,Map<String,String> filters) {
        var paging=page(page,size,search);
        String type="categories";
return categories.findAll(matching(search,type,filters,"name"),paging).map(c -> map("id",c.getId(),"name",c.getName(),"description",c.getDescription(),"image",c.getImage(),"productCount",c.getProductCount(),"brandIds",c.getBrands().stream().map(Brand::getId).toList(),"brandNames",c.getBrands().stream().map(Brand::getName).sorted().toList()));
    }
    @Transactional(readOnly = true)
    public Object detail(long id) {
        String type="categories";
var c=categories.findById(id).orElseThrow(() -> missing(type,id));return map("id",c.getId(),"name",c.getName(),"description",c.getDescription(),"image",c.getImage(),"brandIds",c.getBrands().stream().map(Brand::getId).toList());
    }
    private List<Brand> selectedBrands(List<Long> ids) {
        var result=brands.findAllById(ids);
        if(result.size()!=new HashSet<>(ids).size()) throw new IllegalArgumentException("Thương hiệu không tồn tại");
        return result;
    }
    public Map<String,Object> save(Long id,CategoryInput n,String currentEmail) {
        String type="categories";

                var c=id==null?new Category():categories.lockById(id).orElseThrow(() -> missing(type,id));
                String name=n.name().trim();
                if(id==null?categories.existsByNameIgnoreCase(name):categories.existsByNameIgnoreCaseAndIdNot(name,id))throw new IllegalArgumentException("Danh mục \""+name+"\" đã tồn tại");
                c.setName(name);c.setDescription(optional(n.description()));c.setImage(url(optional(n.image())));
                if(n.brandIds()!=null){
                    var selected=selectedBrands(n.brandIds());
                    if(id!=null&&products.count((root,q,cb)->cb.and(cb.equal(root.get("category").get("id"),id),selected.isEmpty()?cb.isNotNull(root.get("brand")):cb.not(root.get("brand").get("id").in(selected.stream().map(Brand::getId).toList()))))>0)
                        throw new IllegalArgumentException("Không thể bỏ hãng còn sản phẩm trong danh mục. Hãy chuyển sản phẩm sang hãng/danh mục phù hợp trước.");
                    c.setBrands(selected);
                }
                return map("id",categories.save(c).getId());
    }
    public void delete(long id,String currentEmail) {
        String type="categories";
var c=categories.findById(id).orElseThrow(() -> missing(type,id));var refs=new ArrayList<String>();if(c.getProductCount()>0)refs.add("sản phẩm trong danh mục "+c.getName());refs.addAll(vouchers.codesByCategory(id));refs.addAll(promotions.namesByCategory(id));if(!refs.isEmpty())throw new IllegalArgumentException("Danh mục đang được dùng bởi: "+String.join(", ",refs));categories.delete(c);
    }

}
