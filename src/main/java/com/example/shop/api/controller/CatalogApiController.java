package com.example.shop.api.controller;

import java.util.List;
import java.util.Map;

import com.example.shop.api.dto.catalog.ProductSummaryResponse;
import com.example.shop.domain.Banner;
import com.example.shop.domain.Category;
import com.example.shop.service.BannerService;
import com.example.shop.service.CategoryService;
import com.example.shop.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogApiController {
    @org.springframework.beans.factory.annotation.Autowired private com.example.shop.repository.OrderDetailRepository orderDetails;
    @org.springframework.beans.factory.annotation.Autowired private com.example.shop.repository.CategoryRepository categories;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final BannerService bannerService;
    private final com.example.shop.repository.ProductRepository products;
    private final com.example.shop.repository.BrandRepository brands;
    private final java.time.Clock clock;
    public CatalogApiController(ProductService productService, CategoryService categoryService, BannerService bannerService, com.example.shop.repository.ProductRepository products, com.example.shop.repository.BrandRepository brands, java.time.Clock clock) {
        this.productService = productService; this.categoryService = categoryService; this.bannerService = bannerService;
        this.products=products; this.brands=brands; this.clock=clock;
    }
    @GetMapping("/products")
    public org.springframework.data.domain.Page<ProductSummaryResponse> products(@RequestParam(defaultValue="") String search,
            @RequestParam(required=false) Long categoryId, @RequestParam(required=false) Long brandId,
            @RequestParam(required=false) Double minPrice, @RequestParam(required=false) Double maxPrice,
            @RequestParam(defaultValue="false") boolean inStock,
            @RequestParam(required=false) List<Long> brandIds,
            @RequestParam(required=false) Long promotionId,
            @RequestParam(required=false) Integer minWarranty, @RequestParam(required=false) Integer maxWarranty,
            @RequestParam org.springframework.util.MultiValueMap<String,String> parameters,
            @RequestParam(defaultValue="newest") String sort, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="12") int size) {
        if(page<0 || size<1 || size>60 || search.length()>100 || (minPrice!=null && (!Double.isFinite(minPrice) || minPrice<0)) || (maxPrice!=null && (!Double.isFinite(maxPrice) || maxPrice<0)) || (minPrice!=null && maxPrice!=null && minPrice>maxPrice)) throw new IllegalArgumentException("Bộ lọc không hợp lệ");
        var specs = parameters.entrySet().stream().filter(e -> e.getKey().startsWith("spec.")).toList();
        if((minWarranty!=null&&(minWarranty<0||minWarranty>120))||(maxWarranty!=null&&(maxWarranty<0||maxWarranty>120))||(minWarranty!=null&&maxWarranty!=null&&minWarranty>maxWarranty))throw new IllegalArgumentException("Khoảng bảo hành không hợp lệ");
        if (specs.size()>24 || (brandIds!=null && brandIds.size()>60) || specs.stream().anyMatch(e -> e.getKey().length()>105 || e.getKey().length()==5 || e.getValue().size()>60 || e.getValue().stream().anyMatch(v -> v.length()>500))) throw new IllegalArgumentException("Bộ lọc không hợp lệ");
        return products.findAll((root,q,cb) -> {
            // Availability is the primary order, before pagination and the chosen sort.
            if(q.getResultType()!=Long.class && q.getResultType()!=long.class) {
                var available = cb.<Integer>selectCase().when(cb.gt(root.get("quantity"),0),0).otherwise(1);
                var secondary = switch(sort) {
                    case "price-asc" -> cb.asc(root.get("price"));
                    case "price-desc" -> cb.desc(root.get("price"));
                    case "popular" -> cb.desc(root.get("sold"));
                    default -> cb.desc(root.get("id"));
                };
                q.orderBy(cb.asc(available),secondary,cb.desc(root.get("id")));
            }
            var filters=new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            filters.add(cb.isTrue(root.get("active")));filters.add(cb.isTrue(root.get("technologyCatalog")));
            if(!search.isBlank()) filters.add(cb.like(cb.lower(root.get("name")),"%"+search.toLowerCase(java.util.Locale.ROOT)+"%"));
            if(categoryId!=null) filters.add(cb.equal(root.get("category").get("id"),categoryId));
            if(brandId!=null) filters.add(cb.equal(root.get("brand").get("id"),brandId));
            if(brandIds!=null && !brandIds.isEmpty()) filters.add(root.get("brand").get("id").in(brandIds));
            if(inStock) filters.add(cb.gt(root.get("quantity"),0));
            if(minWarranty!=null)filters.add(cb.ge(root.get("warrantyMonths"),minWarranty));
            if(maxWarranty!=null)filters.add(cb.le(root.get("warrantyMonths"),maxWarranty));
            if(promotionId!=null) {
                var sub=q.subquery(Long.class);var offer=sub.from(com.example.shop.domain.Promotion.class);
                var offerProducts=offer.join("products",jakarta.persistence.criteria.JoinType.LEFT);var offerBrands=offer.join("brands",jakarta.persistence.criteria.JoinType.LEFT);var offerCategories=offer.join("categories",jakarta.persistence.criteria.JoinType.LEFT);var now=java.util.Date.from(clock.instant());
                sub.select(offer.get("id")).where(cb.equal(offer.get("id"),promotionId),cb.isTrue(offer.get("active")),
                    cb.lessThan(offer.get("startDate"),now),cb.greaterThan(offer.get("endDate"),now),
                    cb.or(cb.equal(offer.get("scopeType"),com.example.shop.domain.ScopeType.ALL),
                        cb.and(cb.equal(offer.get("scopeType"),com.example.shop.domain.ScopeType.PRODUCT),cb.equal(offerProducts.get("id"),root.get("id"))),
                        cb.and(cb.equal(offer.get("scopeType"),com.example.shop.domain.ScopeType.BRAND),cb.equal(offerBrands.get("id"),root.get("brand").get("id"))),
                        cb.and(cb.equal(offer.get("scopeType"),com.example.shop.domain.ScopeType.CATEGORY),cb.equal(offerCategories.get("id"),root.get("category").get("id")))));
                filters.add(cb.exists(sub));
            }
            for (var entry : specs) {
                var sub = q.subquery(Long.class);
                var spec = sub.from(com.example.shop.domain.ProductSpec.class);
                sub.select(spec.get("id")).where(cb.equal(spec.get("product"),root), cb.equal(spec.get("specName"),entry.getKey().substring(5)), spec.get("specValue").in(entry.getValue()));
                filters.add(cb.exists(sub));
            }
            if(minPrice!=null) filters.add(cb.ge(root.get("price"),minPrice));
            if(maxPrice!=null) filters.add(cb.le(root.get("price"),maxPrice));
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        },org.springframework.data.domain.PageRequest.of(page,size)).map(ProductSummaryResponse::from);
    }
    @GetMapping("/brands") public List<Map<String,Object>> brands() { return brands.findAll().stream().map(b -> Map.<String,Object>of("id",b.getId(),"name",b.getName(),"logoUrl",b.getLogoUrl()==null?"":b.getLogoUrl())).toList(); }
    @GetMapping("/catalog/menu")
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public Map<String,Object> menu(@RequestParam long categoryId) {
        if(categoryId<1)throw new IllegalArgumentException("Danh mục không hợp lệ");
        var result=new java.util.LinkedHashMap<>(facets(categoryId));
        var ranking=orderDetails.topSales(categoryId,org.springframework.data.domain.PageRequest.of(0,10));
        var byId=products.findAllById(ranking.stream().map(r -> r.getProductId()).toList()).stream().collect(java.util.stream.Collectors.toMap(p -> p.getId(),p -> p));
        var selected=new java.util.ArrayList<Map<String,Object>>();
        for(var rank:ranking) {var p=byId.get(rank.getProductId());if(p!=null)selected.add(Map.of("product",ProductSummaryResponse.from(p),"units",rank.getUnits()));}
        if(selected.isEmpty()) {
            var latest=products.findAll((root,q,cb)->{
                if(q.getResultType()!=Long.class)q.orderBy(cb.asc(cb.<Integer>selectCase().when(cb.gt(root.get("quantity"),0),0).otherwise(1)),cb.desc(root.get("id")));
                return cb.and(cb.isTrue(root.get("active")),cb.isTrue(root.get("technologyCatalog")),cb.equal(root.get("category").get("id"),categoryId));
            },org.springframework.data.domain.PageRequest.of(0,10));
            latest.forEach(p -> selected.add(Map.of("product",ProductSummaryResponse.from(p),"units",0L)));
        }
        result.put("bestSellers",selected);result.put("rankingSource",ranking.isEmpty()?"newest":"sales");return result;
    }
    /** Facet counts describe the category before other filters; inactive products never contribute. */
    @GetMapping("/catalog/facets")
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public Map<String,Object> facets(@RequestParam(required=false) Long categoryId) {
        var items = products.findAll((root,q,cb) -> categoryId==null
            ? cb.and(cb.isTrue(root.get("active")),cb.isTrue(root.get("technologyCatalog")))
            : cb.and(cb.isTrue(root.get("active")),cb.isTrue(root.get("technologyCatalog")),cb.equal(root.get("category").get("id"),categoryId)));
        var brandMap = new java.util.TreeMap<String,Map<String,Object>>();
        var assigned=categoryId==null?brands.findAll():categories.findById(categoryId).orElseThrow(()->new IllegalArgumentException("Danh mục không tồn tại")).getBrands();
        for(var b:assigned)brandMap.put(b.getName()+"#"+b.getId(),new java.util.LinkedHashMap<>(Map.of("id",b.getId(),"name",b.getName(),"logoUrl",b.getLogoUrl()==null?"":b.getLogoUrl(),"count",0L)));
        var attributes = new java.util.TreeMap<String,java.util.Map<String,Long>>();
        for (var p : items) {
            if(p.getBrand()!=null) {
                var b=p.getBrand(); var key=b.getName()+"#"+b.getId();
                var row=brandMap.get(key);if(row!=null)row.put("count",(Long)row.get("count")+1);
            }
            if(categoryId!=null) {
                var unique = new java.util.HashSet<String>();
                for(var s:p.getSpecs()) if(s.getSpecName()!=null && s.getSpecValue()!=null && !java.util.Set.of("phiên bản","kết nối","bảo hành","version","connection","warranty").contains(s.getSpecName().trim().toLowerCase(java.util.Locale.ROOT)) && !s.getSpecName().isBlank() && !s.getSpecValue().isBlank() && unique.add(s.getSpecName()+"\u0000"+s.getSpecValue()))
                    attributes.computeIfAbsent(s.getSpecName(),k -> new java.util.TreeMap<>()).merge(s.getSpecValue(),1L,Long::sum);
            }
        }
        var specs=attributes.entrySet().stream().map(e -> Map.of("name",e.getKey(),"values",e.getValue().entrySet().stream().map(v -> Map.of("value",v.getKey(),"count",v.getValue())).toList())).toList();
        return Map.of("brands",brandMap.values(),"specs",specs,"minPrice",items.stream().mapToDouble(p -> p.getPrice()).min().orElse(0),"maxPrice",items.stream().mapToDouble(p -> p.getPrice()).max().orElse(0),"inStockCount",items.stream().filter(p -> p.getQuantity()>0).count(),"total",items.size(),"minWarranty",items.stream().filter(p->p.getWarrantyMonths()!=null).mapToInt(p->p.getWarrantyMonths()).min().orElse(0),"maxWarranty",items.stream().filter(p->p.getWarrantyMonths()!=null).mapToInt(p->p.getWarrantyMonths()).max().orElse(0));
    }
    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return categoryService.getAllCategories(null).stream().filter(c -> c.getProducts()!=null && c.getProducts().stream().anyMatch(p -> p.isActive() && p.isTechnologyCatalog())).map(this::category).toList();
    }
    @GetMapping("/banners")
    public List<Map<String, Object>> banners() {
        return bannerService.getActiveBanners().stream().filter(Banner::isTechnologyCatalog).sorted(java.util.Comparator.comparingLong(Banner::getId).reversed()).map(this::banner).toList();
    }
    private Map<String, Object> category(Category item) { return Map.of("id", item.getId(), "name", item.getName(), "image", item.getImage() == null ? "" : item.getImage(), "productCount", item.getProductCount()); }
    private Map<String, Object> banner(Banner item) { return Map.of("id", item.getId(), "name", item.getName(), "imageUrl", item.getImageUrl() == null ? "" : item.getImageUrl(), "link", item.getLink() == null ? "" : item.getLink()); }
}
