package com.example.shop.api.service;

import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Transactional
public class VariantService {
    @PersistenceContext private EntityManager em;
    private final ProductVariantRepository variants;
    private final ProductRepository products;
    private final OrderDetailRepository details;
    public VariantService(ProductVariantRepository variants,ProductRepository products,OrderDetailRepository details){this.variants=variants;this.products=products;this.details=details;}

    public void save(Product p,JsonNode input){
        if(!input.isArray() || input.size()>100)throw new IllegalArgumentException("Tối đa 100 phiên bản cho một sản phẩm");
        if(!p.hasVariants() && !input.isEmpty() && details.hasOpenSimpleOrders(p.getId()))throw new IllegalArgumentException("Hoàn tất hoặc hủy các đơn đang xử lý trước khi thêm phiên bản");
        Set<Long> seen=new HashSet<>();Set<ProductVariant> retained=Collections.newSetFromMap(new IdentityHashMap<>());Set<String> skus=new HashSet<>();Set<String> combinations=new HashSet<>();
        for(JsonNode row:input){
            ProductVariant v;
            if(row.hasNonNull("id")){
                long id=row.path("id").asLong();
                if(!seen.add(id))throw new IllegalArgumentException("Phiên bản bị trùng");
                v=p.getVariants().stream().filter(x->Objects.equals(x.getId(),id)).findFirst().orElseThrow(()->new IllegalArgumentException("Phiên bản không thuộc sản phẩm"));
            }else{v=new ProductVariant();v.setProduct(p);}
            String sku=text(row.path("sku"),80);
            if(!skus.add(sku.toUpperCase(Locale.ROOT)) || sku.equalsIgnoreCase(p.getSku()) || products.existsBySku(sku))throw new IllegalArgumentException("SKU phiên bản bị trùng với sản phẩm hoặc phiên bản khác");
            v.setSku(sku);
            v.setPrice(number(row,"price",1000000000));v.setQuantity((long)number(row,"quantity",1000000));v.setActive(row.path("active").asBoolean(true));
            String image=row.path("imageUrl").asText("").trim();
            if(image.length()>500 || !(image.isEmpty() || image.startsWith("https://") || (image.startsWith("/")&&!image.startsWith("//")&&!image.contains("\\"))))throw new IllegalArgumentException("Đường dẫn ảnh phiên bản không hợp lệ");
            v.setImageUrl(image);
            JsonNode options=row.path("options");
            if(!options.isObject() || options.isEmpty() || options.size()>6)throw new IllegalArgumentException("Mỗi phiên bản cần 1–6 thuộc tính");
            var cleaned=new TreeMap<String,String>();var names=new HashSet<String>();
            options.fields().forEachRemaining(e->{String name=e.getKey().trim();if(name.isEmpty()||name.length()>80||!names.add(name.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Tên thuộc tính không hợp lệ hoặc bị trùng");cleaned.put(name,text(e.getValue(),100));});
            String signature=cleaned.entrySet().stream().map(e->List.of(e.getKey().toLowerCase(Locale.ROOT),e.getValue().toLowerCase(Locale.ROOT))).toList().toString();
            if(!combinations.add(signature))throw new IllegalArgumentException("Các phiên bản không được trùng tổ hợp thuộc tính");
            v.getOptions().clear();v.getOptions().putAll(cleaned);
            retained.add(v);
            if(!p.getVariants().contains(v))p.getVariants().add(v);
        }
        for(var v:p.getVariants())if(!retained.contains(v))v.setActive(false);
        if(p.hasVariants()){
            p.setQuantity(p.getVariants().stream().filter(ProductVariant::isActive).mapToLong(ProductVariant::getQuantity).sum());
            p.getVariants().stream().filter(ProductVariant::isActive).mapToDouble(ProductVariant::getPrice).min().ifPresent(p::setPrice);
        }
    }
    // Every stock mutation first locks the parent product, serializing its variants as well.
    public ProductVariant require(Product p,Long id){
        if(id==null){if(p.hasVariants())throw new IllegalArgumentException("Vui lòng chọn phiên bản sản phẩm");return null;}
        var v=variants.findById(id).orElseThrow(()->new IllegalArgumentException("Phiên bản không tồn tại"));
        if(v.getProduct().getId()!=p.getId())throw new IllegalArgumentException("Phiên bản không thuộc sản phẩm");
        em.refresh(v,LockModeType.PESSIMISTIC_WRITE);
        return v;
    }
    public static long stock(Product p,ProductVariant v){return v==null?p.getQuantity():v.getQuantity();}
    public static double price(Product p,ProductVariant v){return p.discountedPrice(v==null?p.getPrice():v.getPrice());}
    public static void check(Product p,ProductVariant v,long quantity){
        if(!p.isActive()||!p.isTechnologyCatalog()||(v==null&&p.hasVariants())||(v!=null&&!v.isActive())||quantity<1||quantity>100||quantity>stock(p,v))throw new IllegalArgumentException("Chọn phiên bản hợp lệ và số lượng trong tồn kho: "+p.getName());
    }
    public void restore(OrderDetail d){
        em.flush();
        Product p=products.lockById(d.getProduct().getId()).orElseThrow();em.refresh(p,LockModeType.PESSIMISTIC_WRITE);
        if(d.getVariant()!=null){
            var v=require(p,d.getVariant().getId());v.setQuantity(v.getQuantity()+d.getQuantity());
            if(v.isActive())p.setQuantity(p.getQuantity()+d.getQuantity());
        }else p.setQuantity(p.getQuantity()+d.getQuantity());
        p.setSold(Math.max(0,p.getSold()-d.getQuantity()));
    }
    private static String text(JsonNode n,int max){String value=n.isTextual()?n.asText().trim():"";if(value.isEmpty()||value.length()>max)throw new IllegalArgumentException("SKU hoặc thuộc tính phiên bản không hợp lệ");return value;}
    private static double number(JsonNode n,String field,double max){double value=n.path(field).asDouble(Double.NaN);if(!Double.isFinite(value)||value<0||value>max||value!=Math.rint(value))throw new IllegalArgumentException("Giá/tồn kho phiên bản phải là số nguyên không âm");return value;}
}
