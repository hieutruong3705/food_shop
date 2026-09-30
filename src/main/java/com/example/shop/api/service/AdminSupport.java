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


public final class AdminSupport {
    private AdminSupport() {}
    public static PageRequest page(int page, int size, String search) {
        if (page < 0 || size < 1 || size > 100 || search.length() > 100) throw new IllegalArgumentException("Phân trang không hợp lệ");
        return PageRequest.of(page, size, Sort.by("id").descending());
    }
    public static <T> List<T> list(List<T> value) { return value == null ? List.of() : value; }
    public static String optional(String value) { return value == null ? "" : value; }
    public static boolean enabled(Boolean value) { return value == null || value; }
    public static Date date(String value) {
        try { return Date.from(java.time.Instant.parse(value)); }
        catch (Exception e) { throw new IllegalArgumentException("Ngày không hợp lệ"); }
    }
    public static Date optionalDate(String value) { return value == null || value.isBlank() ? null : date(value); }
    public static String url(String value) {
        value = optional(value);
        if (value.isBlank() || value.startsWith("https://") || (value.startsWith("/") && !value.startsWith("//") && !value.contains("\\"))) return value;
        throw new IllegalArgumentException("Đường dẫn phải bắt đầu bằng / hoặc https://");
    }
    public static ResourceNotFoundException missing(String type,long id) { return new ResourceNotFoundException(type,id); }
    public static Map<String,Object> map(Object... values) {
        var result = new LinkedHashMap<String,Object>();
        for(int i=0;i<values.length;i+=2) result.put((String)values[i],values[i+1]);
        return result;
    }
    public static <T> org.springframework.data.jpa.domain.Specification<T> matching(String value,String type,Map<String,String> filters,String... fields){return (root,q,cb)->{
        var all=new ArrayList<jakarta.persistence.criteria.Predicate>();
        if(!value.isBlank()){var terms=new ArrayList<jakarta.persistence.criteria.Predicate>();for(String field:fields){jakarta.persistence.criteria.Path<?> path=root;for(String part:field.split("\\."))path=path.get(part);terms.add(cb.like(cb.lower(path.as(String.class)),"%"+value.toLowerCase(Locale.ROOT)+"%"));}all.add(cb.or(terms.toArray(jakarta.persistence.criteria.Predicate[]::new)));}
        if(type.equals("users")){
            String role=filters.getOrDefault("role","");if(!role.isBlank()){if(!Set.of("USER","ADMIN").contains(role))throw new IllegalArgumentException("Quyền không hợp lệ");all.add(cb.equal(root.get("role").get("name"),role));}
            String locked=filters.getOrDefault("locked","");if(!locked.isBlank()){if(!Set.of("true","false").contains(locked))throw new IllegalArgumentException("Trạng thái khóa không hợp lệ");all.add(cb.equal(root.get("isLocked"),Boolean.parseBoolean(locked)));}
        }
        if(type.equals("reviews") && !filters.getOrDefault("rating","").isBlank()){int rating=Integer.parseInt(filters.get("rating"));if(rating<1||rating>5)throw new IllegalArgumentException("Số sao không hợp lệ");all.add(cb.equal(root.get("rating"),rating));}
        if(type.equals("products")){
            for(String field:List.of("categoryId","brandId"))if(!filters.getOrDefault(field,"").isBlank()){long id=Long.parseLong(filters.get(field));if(id<1)throw new IllegalArgumentException("ID không hợp lệ");all.add(cb.equal(root.get(field.equals("categoryId")?"category":"brand").get("id"),id));}
            String state=filters.getOrDefault("status","");if(!state.isBlank()&&!state.equals("all")){if(!Set.of("active","inactive").contains(state))throw new IllegalArgumentException("Trạng thái không hợp lệ");all.add(cb.equal(root.get("active"),state.equals("active")));}
        }
        if(type.equals("vouchers")&&!filters.getOrDefault("categoryId","").isBlank()){long id=Long.parseLong(filters.get("categoryId"));if(id==-1)all.add(cb.equal(root.get("scopeType"),ScopeType.ALL));else{if(id<1)throw new IllegalArgumentException("Danh mục không hợp lệ");var byCategory=q.subquery(Long.class);var vc=byCategory.from(com.example.shop.domain.Voucher.class);byCategory.select(vc.get("id")).where(cb.equal(vc.join("categories").get("id"),id));var byProduct=q.subquery(Long.class);var vp=byProduct.from(com.example.shop.domain.Voucher.class);byProduct.select(vp.get("id")).where(cb.equal(vp.join("products").get("category").get("id"),id));all.add(cb.or(cb.equal(root.get("scopeType"),ScopeType.ALL),root.get("id").in(byCategory),root.get("id").in(byProduct)));}}
        return cb.and(all.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };}

}
