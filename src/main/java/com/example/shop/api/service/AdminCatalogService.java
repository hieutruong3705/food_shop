package com.example.shop.api.service;

import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.api.dto.CommerceResponse;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Transactional
public class AdminCatalogService {
    @org.springframework.beans.factory.annotation.Autowired private VariantService variantService;
    @org.springframework.beans.factory.annotation.Autowired private OfferScopeService offerScopes;
    @org.springframework.beans.factory.annotation.Autowired private PromotionPricing pricing;
    @org.springframework.beans.factory.annotation.Autowired private SkuService skuService;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final ProductRepository products; private final CategoryRepository categories; private final BrandRepository brands;
    private final BannerRepository banners; private final VoucherRepository vouchers; private final PromotionRepository promotions;
    private final UserRepository users; private final RoleRepository roles; private final ReviewRepository reviews;
    public AdminCatalogService(ProductRepository p,CategoryRepository c,BrandRepository b,BannerRepository ba,VoucherRepository v,PromotionRepository pr,UserRepository u,RoleRepository ro,ReviewRepository re) {
        products=p;categories=c;brands=b;banners=ba;vouchers=v;promotions=pr;users=u;roles=ro;reviews=re;
    }
    public Page<?> list(String type,int page,int size,String search,Map<String,String> filters) {
        if(page<0 || size<1 || size>100 || search.length()>100) throw new IllegalArgumentException("Phân trang không hợp lệ");
        var paging=PageRequest.of(page,size,Sort.by("id").descending());
        return switch(type) {
            case "products" -> products.findAll(this.<Product>matching(search,type,filters,"name","sku").and((root,q,cb) -> {
                if(q.getResultType()!=Long.class && q.getResultType()!=long.class)
                    q.orderBy(cb.asc(cb.<Integer>selectCase().when(cb.gt(root.get("quantity"),0),0).otherwise(1)),cb.desc(root.get("id")));
                return cb.conjunction();
            }),PageRequest.of(page,size)).map(this::product);
            case "categories" -> categories.findAll(matching(search,type,filters,"name"),paging).map(c -> map("id",c.getId(),"name",c.getName(),"description",c.getDescription(),"image",c.getImage(),"brandIds",c.getBrands().stream().map(Brand::getId).toList(),"brandNames",c.getBrands().stream().map(Brand::getName).sorted().toList()));
            case "brands" -> brands.findAll(matching(search,type,filters,"name"),paging).map(b -> map("id",b.getId(),"name",b.getName(),"description",b.getDescription(),"logoUrl",b.getLogoUrl()));
            case "banners" -> banners.findAll(matching(search,type,filters,"name"),paging).map(b -> map("id",b.getId(),"name",b.getName(),"imageUrl",b.getImageUrl(),"link",b.getLink(),"active",b.isActive(),"technologyCatalog",b.isTechnologyCatalog()));
            case "vouchers" -> vouchers.findAll(matching(search,type,filters,"code"),paging).map(this::voucher);
            case "promotions" -> promotions.findAll(matching(search,type,filters,"name"),paging).map(this::promotion);
            case "users" -> users.findAll(matching(search,type,filters,"fullName","email"),paging).map(u -> map("id",u.getId(),"email",u.getEmail(),"fullName",u.getFullName(),"role",u.getRole().getName(),"enabled",u.isEnabled(),"locked",u.getIsLocked()));
            case "reviews" -> reviews.findAll(matching(search,type,filters,"content","user.fullName","user.email","product.name"),paging).map(CommerceResponse::review);
            default -> throw missing(type,0);
        };
    }
    public Map<String,Object> product(Product p) { return map("id",p.getId(),"name",p.getName(),"sku",p.getSku(),"slug",p.getSlug(),"price",p.getPrice(),"quantity",p.getQuantity(),"active",p.isActive(),"technologyCatalog",p.isTechnologyCatalog(),"shortDesc",p.getShortDesc(),"detailDesc",p.getDetailDesc(),"warrantyMonths",p.getWarrantyMonths(),"categoryId",p.getCategory()==null?null:p.getCategory().getId(),"brandId",p.getBrand()==null?null:p.getBrand().getId(),"images",p.getImages().stream().map(ProductImage::getImageUrl).toList(),"variants",p.getVariants().stream().map(v -> com.example.shop.api.dto.catalog.VariantResponse.from(v,pricing.current())).toList(),"specs",p.getSpecs().stream().map(s -> map("name",s.getSpecName(),"value",s.getSpecValue())).toList()); }
    public Map<String,Object> save(String type,Long id,JsonNode n,String currentEmail) {
        long saved=switch(type) {
            case "products" -> {
                Product p=id==null?new Product():products.lockById(id).orElseThrow(() -> missing(type,id));
                p.setTechnologyCatalog(n.path("technologyCatalog").asBoolean(true));p.setName(text(n,"name",150));skuService.checkProduct(text(n,"sku",80),p.getId());p.setSku(text(n,"sku",80));p.setSlug(text(n,"slug",150));
                if(!p.getSlug().matches("[a-z0-9-]+")) throw new IllegalArgumentException("Slug chỉ gồm chữ thường, số và dấu gạch ngang");
                p.setPrice(number(n,"price",0,1000000000));p.setQuantity(integer(n,"quantity",0,1000000));p.setActive(n.path("active").asBoolean(true));
                p.setShortDesc(optional(n,"shortDesc",2000));p.setDetailDesc(optional(n,"detailDesc",20000));p.setWarrantyMonths((int)integer(n,"warrantyMonths",0,120));
                p.setCategory(categories.lockById(n.path("categoryId").asLong()).orElseThrow(() -> new IllegalArgumentException("Chọn danh mục hợp lệ")));
                p.setBrand(brands.findById(n.path("brandId").asLong()).orElseThrow(() -> new IllegalArgumentException("Chọn thương hiệu hợp lệ")));
                if(p.getCategory().getBrands().stream().noneMatch(b->b.getId()==p.getBrand().getId()))throw new IllegalArgumentException("Thương hiệu không thuộc danh mục đã chọn. Hãy gán thương hiệu trong quản trị Danh mục.");
                p.getImages().clear();if(n.path("images").size()>12)throw new IllegalArgumentException("Tối đa 12 ảnh");
                for(JsonNode value:n.path("images")) { var image=new ProductImage();image.setProduct(p);image.setSortOrder(p.getImages().size());image.setImageUrl(url(value.asText()));p.getImages().add(image); }
                p.getSpecs().clear();if(n.path("specs").size()>40)throw new IllegalArgumentException("Tối đa 40 thông số");
                for(JsonNode value:n.path("specs")) { var spec=new ProductSpec();spec.setProduct(p);spec.setSpecName(text(value,"name",100));spec.setSpecValue(text(value,"value",500));p.getSpecs().add(spec); }
                if(n.has("variants"))variantService.save(p,n.get("variants"));
                else if(p.hasVariants())throw new IllegalArgumentException("Gửi danh sách phiên bản khi cập nhật sản phẩm có tùy chọn");
                yield products.save(p).getId();
            }
            case "categories" -> {
                var c=id==null?new Category():categories.lockById(id).orElseThrow(() -> missing(type,id));
                c.setName(text(n,"name",100));c.setDescription(optional(n,"description",5000));c.setImage(url(n.path("image").asText("")));
                if(n.has("brandIds")){
                    var selected=selectedBrands(n);
                    if(id!=null&&products.count((root,q,cb)->cb.and(cb.equal(root.get("category").get("id"),id),selected.isEmpty()?cb.isNotNull(root.get("brand")):cb.not(root.get("brand").get("id").in(selected.stream().map(Brand::getId).toList()))))>0)
                        throw new IllegalArgumentException("Không thể bỏ hãng còn sản phẩm trong danh mục. Hãy chuyển sản phẩm sang hãng/danh mục phù hợp trước.");
                    c.setBrands(selected);
                }
                yield categories.save(c).getId();
            }
            case "brands" -> {var b=id==null?new Brand():brands.findById(id).orElseThrow(() -> missing(type,id));b.setName(text(n,"name",100));b.setDescription(optional(n,"description",5000));b.setLogoUrl(url(n.path("logoUrl").asText("")));yield brands.save(b).getId();}
            case "banners" -> {var b=id==null?new Banner():banners.findById(id).orElseThrow(() -> missing(type,id));b.setTechnologyCatalog(n.path("technologyCatalog").asBoolean(true));b.setName(text(n,"name",100));b.setImageUrl(url(text(n,"imageUrl",500)));b.setLink(url(n.path("link").asText("/products")));b.setActive(n.path("active").asBoolean(true));yield banners.save(b).getId();}
            case "vouchers" -> {
                var v=id==null?new Voucher():vouchers.findById(id).orElseThrow(() -> missing(type,id));String code=text(n,"code",50).toUpperCase(Locale.ROOT);
                var duplicate=vouchers.findByCode(code);if(duplicate!=null && duplicate.getId()!=v.getId())throw new IllegalArgumentException("Mã voucher đã tồn tại");
                if(id!=null)entityManager.refresh(v,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                if(n.has("active"))v.setActive(n.path("active").asBoolean());
                if(n.has("startDate"))v.setStartDate(optionalDate(n,"startDate"));
                if(n.has("endDate"))v.setEndDate(optionalDate(n,"endDate"));
                if(v.getStartDate()!=null && v.getEndDate()!=null && !v.getEndDate().after(v.getStartDate()))throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
                if(n.has("minimumOrder"))v.setMinimumOrder(integer(n,"minimumOrder",0,100000000000L));
                if(n.has("maximumDiscount"))v.setMaximumDiscount(n.path("maximumDiscount").isNull()?null:(double)integer(n,"maximumDiscount",1,100000000000L));
                if(n.has("usageLimit"))v.setUsageLimit(n.path("usageLimit").isNull()?null:integer(n,"usageLimit",1,1000000000));
                v.setCode(code);v.setDiscount(number(n,"discount",1,100));offerScopes.apply(v,n,true);yield vouchers.save(v).getId();
            }
            case "promotions" -> {
                var p=id==null?new Promotion():promotions.findById(id).orElseThrow(() -> missing(type,id));p.setName(text(n,"name",100));p.setDescription(optional(n,"description",2000));p.setDiscountRate(number(n,"discountRate",1,100));
                p.setStartDate(date(n,"startDate"));p.setEndDate(date(n,"endDate"));if(!p.getEndDate().after(p.getStartDate()))throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
                p.setActive(n.path("active").asBoolean(true));offerScopes.apply(p,n,false);yield promotions.save(p).getId();
            }
            case "users" -> {
                if(id==null)throw new IllegalArgumentException("Người dùng tạo tài khoản qua đăng ký");
                var u=users.findById(id).orElseThrow(() -> missing(type,id));if(u.getEmail().equals(currentEmail))throw new IllegalArgumentException("Không đổi quyền hoặc khóa chính tài khoản đang dùng");
                String role=text(n,"role",10);if(!Set.of("USER","ADMIN").contains(role))throw new IllegalArgumentException("Quyền không hợp lệ");
                u.setRole(roles.findByName(role));u.setIsLocked(n.path("locked").asBoolean());u.setEnabled(n.path("enabled").asBoolean());yield u.getId();
            }
            default -> throw missing(type,id==null?0:id);
        };
        return map("id",saved);
    }
    public Object detail(String type,long id) {
        return switch(type) {
            case "products" -> product(products.findById(id).orElseThrow(() -> missing(type,id)));
            case "categories" -> {var c=categories.findById(id).orElseThrow(() -> missing(type,id));yield map("id",c.getId(),"name",c.getName(),"description",c.getDescription(),"image",c.getImage(),"brandIds",c.getBrands().stream().map(Brand::getId).toList());}
            case "brands" -> {var b=brands.findById(id).orElseThrow(() -> missing(type,id));yield map("id",b.getId(),"name",b.getName(),"description",b.getDescription(),"logoUrl",b.getLogoUrl());}
            case "banners" -> {var b=banners.findById(id).orElseThrow(() -> missing(type,id));yield map("id",b.getId(),"name",b.getName(),"imageUrl",b.getImageUrl(),"link",b.getLink(),"active",b.isActive(),"technologyCatalog",b.isTechnologyCatalog());}
            case "vouchers" -> voucher(vouchers.findById(id).orElseThrow(() -> missing(type,id)));
            case "promotions" -> promotion(promotions.findById(id).orElseThrow(() -> missing(type,id)));
            case "users" -> {var u=users.findById(id).orElseThrow(() -> missing(type,id));yield map("id",u.getId(),"email",u.getEmail(),"fullName",u.getFullName(),"role",u.getRole().getName(),"enabled",u.isEnabled(),"locked",u.getIsLocked());}
            case "reviews" -> CommerceResponse.review(reviews.findById(id).orElseThrow(() -> missing(type,id)));
            default -> throw missing(type,id);
        };
    }
    public Map<String,Object> selling(long id, boolean active) {
        var product=products.lockById(id).orElseThrow(() -> missing("products",id));
        product.setActive(active);
        return map("id",product.getId(),"active",product.isActive(),"quantity",product.getQuantity());
    }
    private List<Brand> selectedBrands(JsonNode n){var ids=ids(n,"brandIds");var result=brands.findAllById(ids);if(result.size()!=new HashSet<>(ids).size())throw new IllegalArgumentException("Thương hiệu không tồn tại");return result;}
    private Map<String,Object> offerView(ScopedOffer offer,Map<String,Object> base){
        base.put("scopeType",offer.getScopeType());base.put("categoryIds",offer.getCategories().stream().map(Category::getId).toList());
        base.put("brandIds",offer.getBrands().stream().map(Brand::getId).toList());base.put("productIds",offer.getProducts().stream().map(Product::getId).toList());return base;
    }
    private Map<String,Object> voucher(Voucher v){return offerView(v,map("id",v.getId(),"code",v.getCode(),"discount",v.getDiscount(),"all",v.isAll(),"active",v.isActive(),"startDate",v.getStartDate(),"endDate",v.getEndDate(),"minimumOrder",v.getMinimumOrder(),"maximumDiscount",v.getMaximumDiscount(),"usageLimit",v.getUsageLimit(),"usedCount",v.getUsedCount()));}
    private Map<String,Object> promotion(Promotion p){return offerView(p,map("id",p.getId(),"name",p.getName(),"description",p.getDescription(),"discountRate",p.getDiscountRate(),"startDate",p.getStartDate(),"endDate",p.getEndDate(),"active",p.isActive()));}
    public Page<Map<String,Object>> options(String type,int page,String search,Long categoryId,List<Long> categoryIds,List<Long> selected){
        if(page<0||search.length()>100||selected.size()>500||categoryIds.size()>500)throw new IllegalArgumentException("Bộ chọn không hợp lệ");
        var paging=PageRequest.of(page,20,Sort.by("name").ascending().and(Sort.by("id")));
        org.springframework.data.jpa.domain.Specification<Brand> brandSpec=(root,q,cb)->{
            var filters=new ArrayList<jakarta.persistence.criteria.Predicate>();
            filters.add(cb.like(cb.lower(root.get("name")),"%"+search.toLowerCase(Locale.ROOT)+"%"));
            if(!selected.isEmpty())filters.add(root.get("id").in(selected));
            if(categoryId!=null||!categoryIds.isEmpty()){var sub=q.subquery(Long.class);var c=sub.from(Category.class);var b=c.join("brands");sub.select(b.get("id")).where(c.get("id").in(categoryId==null?categoryIds:List.of(categoryId)));filters.add(root.get("id").in(sub));}
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return switch(type){
            case "brands" -> brands.findAll(brandSpec,paging).map(b->map("id",b.getId(),"name",b.getName()));
            case "categories" -> categories.findAll((root,q,cb)->cb.and(cb.like(cb.lower(root.get("name")),"%"+search.toLowerCase(Locale.ROOT)+"%"),selected.isEmpty()?cb.conjunction():root.get("id").in(selected)),paging).map(c->map("id",c.getId(),"name",c.getName()));
            case "products" -> products.findAll((root,q,cb)->cb.and(cb.or(cb.like(cb.lower(root.get("name")),"%"+search.toLowerCase(Locale.ROOT)+"%"),cb.like(cb.lower(root.get("sku")),"%"+search.toLowerCase(Locale.ROOT)+"%")),selected.isEmpty()?cb.conjunction():root.get("id").in(selected)),paging).map(p->map("id",p.getId(),"name",p.getName(),"sku",p.getSku(),"price",p.getPrice(),"image",p.getImage(),"brand",p.getBrand()==null?null:p.getBrand().getName(),"category",p.getCategory()==null?null:p.getCategory().getName(),"quantity",p.getQuantity()));
            default -> throw new IllegalArgumentException("Bộ chọn không hợp lệ");
        };
    }
    public void delete(String type,long id,String currentEmail) {
        switch(type) {
            case "products" -> selling(id,false);
            case "categories" -> {var c=categories.findById(id).orElseThrow(() -> missing(type,id));var refs=new ArrayList<String>();if(c.getProductCount()>0)refs.add("sản phẩm trong danh mục "+c.getName());refs.addAll(vouchers.codesByCategory(id));refs.addAll(promotions.namesByCategory(id));if(!refs.isEmpty())throw new IllegalArgumentException("Danh mục đang được dùng bởi: "+String.join(", ",refs));categories.delete(c);}
            case "brands" -> {var b=brands.findById(id).orElseThrow(() -> missing(type,id));var refs=new ArrayList<String>();b.getProducts().stream().map(Product::getName).sorted().forEach(refs::add);refs.addAll(vouchers.codesByBrand(id));refs.addAll(promotions.namesByBrand(id));refs.addAll(categories.namesByBrand(id));if(!refs.isEmpty())throw new IllegalArgumentException("Thương hiệu đang được dùng bởi: "+String.join(", ",refs));brands.delete(b);}
            case "banners" -> banners.deleteById(id);
            case "vouchers" -> vouchers.deleteById(id);
            case "promotions" -> promotions.deleteById(id);
            case "reviews" -> reviews.deleteById(id);
            case "users" -> {var u=users.findById(id).orElseThrow(() -> missing(type,id));if(u.getEmail().equals(currentEmail))throw new IllegalArgumentException("Không thể khóa chính mình");if(u.getOrders()!=null && !u.getOrders().isEmpty())throw new IllegalArgumentException("Không xóa tài khoản đã có đơn hàng; hãy dùng chức năng khóa");users.delete(u);}
            default -> throw missing(type,id);
        }
    }
    private static <T> org.springframework.data.jpa.domain.Specification<T> matching(String value,String type,Map<String,String> filters,String... fields){return (root,q,cb)->{
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
        if(type.equals("vouchers")&&!filters.getOrDefault("categoryId","").isBlank()){long id=Long.parseLong(filters.get("categoryId"));if(id==-1)all.add(cb.equal(root.get("scopeType"),ScopeType.ALL));else{if(id<1)throw new IllegalArgumentException("Danh mục không hợp lệ");q.distinct(true);all.add(cb.or(cb.equal(root.get("scopeType"),ScopeType.ALL),cb.equal(root.join("categories",jakarta.persistence.criteria.JoinType.LEFT).get("id"),id)));}}
        return cb.and(all.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };}
    private static String text(JsonNode n,String field,int max){String value=n.path(field).asText("").trim();if(value.isEmpty() || value.length()>max)throw new IllegalArgumentException("Dữ liệu không hợp lệ: "+field);return value;}
    private static String optional(JsonNode n,String field,int max){String value=n.path(field).asText("");if(value.length()>max)throw new IllegalArgumentException("Nội dung quá dài: "+field);return value;}
    private static double number(JsonNode n,String field,double min,double max){double value=n.path(field).asDouble(Double.NaN);if(!Double.isFinite(value) || value<min || value>max)throw new IllegalArgumentException("Số không hợp lệ: "+field);return value;}
    private static long integer(JsonNode n,String field,long min,long max){double value=number(n,field,min,max);if(value!=Math.rint(value))throw new IllegalArgumentException("Yêu cầu số nguyên: "+field);return (long)value;}
    private static Date date(JsonNode n,String field){try{return Date.from(java.time.Instant.parse(text(n,field,40)));}catch(Exception e){throw new IllegalArgumentException("Ngày không hợp lệ: "+field);}}
    private static Date optionalDate(JsonNode n,String field){return n.path(field).isNull() || n.path(field).asText("").isBlank()?null:date(n,field);}
    private static List<Long> ids(JsonNode n,String field){var result=new ArrayList<Long>();for(JsonNode value:n.path(field)){if(!value.canConvertToLong() || value.asLong()<1)throw new IllegalArgumentException("ID không hợp lệ");result.add(value.asLong());}return result;}
    private static String url(String value){if(value.isBlank() || value.startsWith("https://") || (value.startsWith("/") && !value.startsWith("//") && !value.contains("\\")))return value;throw new IllegalArgumentException("Đường dẫn phải bắt đầu bằng / hoặc https://");}
    private static ResourceNotFoundException missing(String type,long id){return new ResourceNotFoundException(type,id);}
    public static Map<String,Object> map(Object... values){var result=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)result.put((String)values[i],values[i+1]);return result;}
}
