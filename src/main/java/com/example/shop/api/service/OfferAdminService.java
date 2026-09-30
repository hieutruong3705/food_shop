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
public class OfferAdminService {
    private final VoucherRepository vouchers;
    private final PromotionRepository promotions;
    private final OfferScopeService offerScopes;
    private final EntityManager entityManager;
    private final ProductRepository products;
    public OfferAdminService(VoucherRepository vouchers, PromotionRepository promotions, OfferScopeService offerScopes, EntityManager entityManager, ProductRepository products) {
        this.products = products;
        this.vouchers = vouchers;
        this.promotions = promotions;
        this.offerScopes = offerScopes;
        this.entityManager = entityManager;
    }
    @Transactional(readOnly = true)
    public Page<?> listVouchers(int page,int size,String search,Map<String,String> filters) {
        var paging=page(page,size,search);
        String type="vouchers";
return vouchers.findAll(matching(search,type,filters,"code"),paging).map(this::voucher);
    }
    @Transactional(readOnly = true)
    public Object detailVoucher(long id) {
        String type="vouchers";
return voucher(vouchers.findById(id).orElseThrow(() -> missing(type,id)));
    }
    public Map<String,Object> saveVoucher(Long id,VoucherInput n,Set<String> present) {
        String type="vouchers";

                var v=id==null?new Voucher():vouchers.findById(id).orElseThrow(() -> missing(type,id));String code=n.code().trim().toUpperCase(Locale.ROOT);
                var duplicate=vouchers.findByCode(code);if(duplicate!=null && duplicate.getId()!=v.getId())throw new IllegalArgumentException("Mã voucher đã tồn tại");
                if(id!=null)entityManager.refresh(v,LockModeType.PESSIMISTIC_WRITE);
                if(present.contains("active"))v.setActive(Boolean.TRUE.equals(n.active()));
                if(present.contains("startDate"))v.setStartDate(optionalDate(n.startDate()));
                if(present.contains("endDate"))v.setEndDate(optionalDate(n.endDate()));
                if(v.getStartDate()!=null && v.getEndDate()!=null && !v.getEndDate().after(v.getStartDate()))throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
                if(present.contains("minimumOrder"))v.setMinimumOrder(n.minimumOrder());
                if(present.contains("maximumDiscount"))v.setMaximumDiscount(n.maximumDiscount()==null?null:(double)n.maximumDiscount());
                if(present.contains("usageLimit"))v.setUsageLimit(n.usageLimit()==null?null:n.usageLimit());
                v.setCode(code);v.setDiscount(n.discount());offerScopes.apply(v,n,true);return map("id",vouchers.save(v).getId());
    }
    public void deleteVoucher(long id) { vouchers.deleteById(id); }
    @Transactional(readOnly = true)
    public Page<?> listPromotions(int page,int size,String search,Map<String,String> filters) {
        var paging=page(page,size,search);
        String type="promotions";
return promotions.findAll(matching(search,type,filters,"name"),paging).map(this::promotion);
    }
    @Transactional(readOnly = true)
    public Object detailPromotion(long id) {
        String type="promotions";
return promotion(promotions.findById(id).orElseThrow(() -> missing(type,id)));
    }
    public Map<String,Object> savePromotion(Long id,PromotionInput n) {
        String type="promotions";

                var p=id==null?new Promotion():promotions.findById(id).orElseThrow(() -> missing(type,id));p.setName(n.name().trim());p.setDescription(optional(n.description()));p.setDiscountRate(n.discountRate());
                p.setStartDate(date(n.startDate()));p.setEndDate(date(n.endDate()));if(!p.getEndDate().after(p.getStartDate()))throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
                p.setActive(enabled(n.active()));offerScopes.apply(p,n,false);var saved=promotions.save(p);
                return map("id",saved.getId(),"warnings",overlaps(saved));
    }
    /**
     * Active promotions whose time window meets this one and that cover at least one of the same
     * sellable products. Saving is not blocked: shoppers get only the highest rate, so the admin is
     * told which promotions will be outbid (or outbid this one) on those products.
     */
    private List<String> overlaps(Promotion saved) {
        if(!saved.isActive())return List.of();
        entityManager.flush();
        var mine=OfferScopeSpecification.visible(saved);
        var result=new ArrayList<String>();
        for(Promotion other:promotions.findAll()){
            if(other.getId()==saved.getId()||!other.isActive()||other.getStartDate()==null||other.getEndDate()==null)continue;
            if(!other.getStartDate().before(saved.getEndDate())||!saved.getStartDate().before(other.getEndDate()))continue;
            long shared=products.count(mine.and(OfferScopeSpecification.visible(other)));
            if(shared>0)result.add("Trùng "+shared+" sản phẩm và thời gian với khuyến mãi \""+other.getName()+"\" ("+other.getDiscountRate()+"%). Khách chỉ nhận mức giảm cao nhất.");
        }
        return result;
    }
    public void deletePromotion(long id) { promotions.deleteById(id); }
    private Map<String,Object> offerView(ScopedOffer offer,Map<String,Object> base) {
        base.put("scopeType",offer.getScopeType());base.put("scopeDescription",OfferScopeText.describe(offer));base.put("categoryIds",offer.getCategories().stream().map(Category::getId).toList());
        base.put("brandIds",offer.getBrands().stream().map(Brand::getId).toList());base.put("productIds",offer.getProducts().stream().map(Product::getId).toList());return base;
    }
    private Map<String,Object> voucher(Voucher v){return offerView(v,map("id",v.getId(),"code",v.getCode(),"discount",v.getDiscount(),"all",v.isAll(),"active",v.isActive(),"startDate",v.getStartDate(),"endDate",v.getEndDate(),"minimumOrder",v.getMinimumOrder(),"maximumDiscount",v.getMaximumDiscount(),"usageLimit",v.getUsageLimit(),"usedCount",v.getUsedCount()));}
    private Map<String,Object> promotion(Promotion p){return offerView(p,map("id",p.getId(),"name",p.getName(),"description",p.getDescription(),"discountRate",p.getDiscountRate(),"startDate",p.getStartDate(),"endDate",p.getEndDate(),"active",p.isActive()));}

}
