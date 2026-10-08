package com.example.shop.api.service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import com.example.shop.api.dto.catalog.ProductSummaryResponse;
import com.example.shop.domain.Banner;
import com.example.shop.domain.Category;
import com.example.shop.service.BannerService;
import com.example.shop.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@org.springframework.stereotype.Service

@Transactional(readOnly=true)
public class CatalogReadService {
    private final com.example.shop.repository.OrderDetailRepository orderDetails;
    private final com.example.shop.repository.CategoryRepository categories;
    private final CategoryService categoryService;
    private final BannerService bannerService;
    private final com.example.shop.repository.ProductRepository products;
    private final com.example.shop.repository.BrandRepository brands;
    private final java.time.Clock clock;
    private final com.example.shop.api.service.PromotionPricing pricing;
    private final com.example.shop.repository.PromotionRepository promotions;
    private final ProductRatings ratings;
    public CatalogReadService(CategoryService categoryService, BannerService bannerService, com.example.shop.repository.ProductRepository products, com.example.shop.repository.BrandRepository brands, java.time.Clock clock, com.example.shop.api.service.PromotionPricing pricing, com.example.shop.repository.OrderDetailRepository orderDetails, com.example.shop.repository.CategoryRepository categories, com.example.shop.repository.PromotionRepository promotions, ProductRatings ratings) {
this.ratings=ratings;
this.orderDetails=orderDetails;
this.categories=categories;
this.promotions=promotions;
        this.categoryService = categoryService; this.bannerService = bannerService;
        this.products=products; this.brands=brands; this.clock=clock; this.pricing=pricing;
    }
    
    
    public org.springframework.data.domain.Page<ProductSummaryResponse> products(String search,
            Long categoryId, Long brandId,
            Double minPrice, Double maxPrice,
            boolean inStock,
            List<Long> brandIds,
            Long promotionId,
            Integer minWarranty, Integer maxWarranty,
            org.springframework.util.MultiValueMap<String,String> parameters,
            String sort, int page, int size) {
        if(page<0 || size<1 || size>60 || search.length()>100 || (minPrice!=null && (!Double.isFinite(minPrice) || minPrice<0)) || (maxPrice!=null && (!Double.isFinite(maxPrice) || maxPrice<0)) || (minPrice!=null && maxPrice!=null && minPrice>maxPrice)) throw new IllegalArgumentException("Bộ lọc không hợp lệ");
        var specs = parameters.entrySet().stream().filter(e -> e.getKey().startsWith("spec.")).toList();
        if((minWarranty!=null&&(minWarranty<0||minWarranty>120))||(maxWarranty!=null&&(maxWarranty<0||maxWarranty>120))||(minWarranty!=null&&maxWarranty!=null&&minWarranty>maxWarranty))throw new IllegalArgumentException("Khoảng bảo hành không hợp lệ");
        if (specs.size()>24 || (brandIds!=null && brandIds.size()>60) || specs.stream().anyMatch(e -> e.getKey().length()>105 || e.getKey().length()==5 || e.getValue().size()>60 || e.getValue().stream().anyMatch(v -> v.length()>500))) throw new IllegalArgumentException("Bộ lọc không hợp lệ");
        var discounts=pricing.current();
        var runningPromotions=promotionId==null?java.util.List.<com.example.shop.domain.Promotion>of():promotions.findRunning(java.util.Date.from(clock.instant()));
        var found = products.findAll((root,q,cb) -> {
            // Availability is the primary order, before pagination and the chosen sort.
            if(q.getResultType()!=Long.class && q.getResultType()!=long.class) {
                var available = cb.<Integer>selectCase().when(cb.gt(root.get("quantity"),0),0).otherwise(1);
                var secondary = switch(sort) {
                    case "price-asc" -> cb.asc(root.get("price"));
                    case "price-desc" -> cb.desc(root.get("price"));
                    case "popular" -> cb.desc(unitsSold(root,q,cb));
                    // "featured" rải đều các lô hàng nhập cùng đợt: id nhân số nguyên tố rồi lấy dư,
                    // nên thứ tự trông ngẫu nhiên nhưng ổn định giữa các trang, không trùng và không sót.
                    case "featured" -> cb.asc(cb.mod(cb.prod(root.get("id").as(Integer.class),7919),101));
                    default -> cb.desc(root.get("id"));
                };
                q.orderBy(cb.asc(available),secondary,cb.desc(root.get("id")));
            }
            var filters=new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            filters.add(cb.isTrue(root.get("active")));filters.add(cb.isTrue(root.get("technologyCatalog")));
            if(!search.isBlank()) { /* Name, SKU, brand or category; many-to-one joins cannot repeat a product. */ String like="%"+search.trim().toLowerCase(java.util.Locale.ROOT)+"%"; var brand=root.join("brand",jakarta.persistence.criteria.JoinType.LEFT); var category=root.join("category",jakarta.persistence.criteria.JoinType.LEFT); filters.add(cb.or(cb.like(cb.lower(root.get("name")),like),cb.like(cb.lower(root.get("sku")),like),cb.like(cb.lower(brand.get("name")),like),cb.like(cb.lower(category.get("name")),like))); }
            if(categoryId!=null) filters.add(cb.equal(root.get("category").get("id"),categoryId));
            if(brandId!=null) filters.add(cb.equal(root.get("brand").get("id"),brandId));
            if(brandIds!=null && !brandIds.isEmpty()) filters.add(root.get("brand").get("id").in(brandIds));
            if(inStock) filters.add(cb.gt(root.get("quantity"),0));
            if(minWarranty!=null)filters.add(cb.ge(root.get("warrantyMonths"),minWarranty));
            if(maxWarranty!=null)filters.add(cb.le(root.get("warrantyMonths"),maxWarranty));
            if(promotionId!=null) {
                // Same rule as the sale price: a promotion that is not running selects nothing.
                var offer=runningPromotions.stream().filter(p->p.getId()==promotionId).findFirst();
                filters.add(offer.isPresent()?offer.get().scope().predicate(root,cb):cb.disjunction());
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
        },org.springframework.data.domain.PageRequest.of(page,size));
        var scores=ratings.of(found.getContent());
        return found.map(p->ProductSummaryResponse.from(p,discounts,scores));
    }
    /**
     * "Best selling" means units in confirmed, shipping or completed orders that are paid or cash on
     * delivery. The menu ranking ({@code OrderDetailRepository.topSales}) counts the same way, so the
     * sort order and the menu never disagree; {@code products.sold} is not used for ranking.
     */
    private static jakarta.persistence.criteria.Expression<Long> unitsSold(jakarta.persistence.criteria.Root<com.example.shop.domain.Product> root,
            jakarta.persistence.criteria.CriteriaQuery<?> q, jakarta.persistence.criteria.CriteriaBuilder cb) {
        var sub = q.subquery(Long.class);
        var detail = sub.from(com.example.shop.domain.OrderDetail.class);
        var order = detail.join("order");
        sub.select(cb.coalesce(cb.sum(detail.<Long>get("quantity")), 0L)).where(
                cb.equal(detail.get("product"), root),
                order.get("status").in(List.of(com.example.shop.domain.OrderStatus.CONFIRMED, com.example.shop.domain.OrderStatus.SHIPPING, com.example.shop.domain.OrderStatus.COMPLETED)),
                cb.or(cb.equal(order.get("paymentMethod"), "COD"), cb.equal(order.get("paymentStatus"), com.example.shop.domain.PaymentStatus.PAID)));
        return sub;
    }

     public List<Map<String,Object>> brands() { return brands.findAll().stream().map(b -> Map.<String,Object>of("id",b.getId(),"name",b.getName(),"logoUrl",b.getLogoUrl()==null?"":b.getLogoUrl())).toList(); }
    
    
    public Map<String,Object> menu(long categoryId) {
        if(categoryId<1)throw new IllegalArgumentException("Danh mục không hợp lệ");
        var discounts=pricing.current();
        var result=new java.util.LinkedHashMap<>(facets(categoryId));
        var ranking=orderDetails.topSales(categoryId,org.springframework.data.domain.PageRequest.of(0,10));
        var byId=products.findAllById(ranking.stream().map(r -> r.getProductId()).toList()).stream().collect(java.util.stream.Collectors.toMap(p -> p.getId(),p -> p));
        var selected=new java.util.ArrayList<Map<String,Object>>();
        for(var rank:ranking) {var p=byId.get(rank.getProductId());if(p!=null)selected.add(Map.of("product",ProductSummaryResponse.from(p,discounts),"units",rank.getUnits()));}
        // Both lists are always sent: a category with no sales has an empty bestSellers, not the newest products.
        var newest=new java.util.ArrayList<Map<String,Object>>();
        products.findAll((root,q,cb)->{
            if(q.getResultType()!=Long.class)q.orderBy(cb.asc(cb.<Integer>selectCase().when(cb.gt(root.get("quantity"),0),0).otherwise(1)),cb.desc(root.get("id")));
            return cb.and(cb.isTrue(root.get("active")),cb.isTrue(root.get("technologyCatalog")),cb.equal(root.get("category").get("id"),categoryId));
        },org.springframework.data.domain.PageRequest.of(0,5)).forEach(p -> newest.add(Map.of("product",ProductSummaryResponse.from(p,discounts),"units",0L)));
        result.put("bestSellers",selected);result.put("newest",newest);
        // Kept for older clients; the menu no longer switches lists.
        result.put("rankingSource",ranking.isEmpty()?"newest":"sales");return result;
    }
    /** Facet counts describe the category before other filters; inactive products never contribute. */
    
    
    public Map<String,Object> facets(Long categoryId) {
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
    
    /**
     * Categories with something to buy. {@code productCount} is the number a shopper can buy (active and
     * in the technology catalog), the same count the menu, the home page and the admin pickers use;
     * {@link Category#getProductCount()} counts every product and only guards deleting a category.
     */
    public List<Map<String, Object>> categories() {
        var all = categoryService.getAllCategories(null);
        var counts = new java.util.HashMap<Long, Long>();
        if (!all.isEmpty()) for (Object[] row : products.sellableCountsByCategory(all.stream().map(Category::getId).toList())) counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        return all.stream().filter(c -> counts.getOrDefault(c.getId(), 0L) > 0).map(c -> category(c, counts.get(c.getId()))).toList();
    }
    
    /** Banners of one placement; no placement asked means the home hero, which is what the old call returned. */
    public List<Map<String, Object>> banners(com.example.shop.domain.BannerPlacement placement) {
        var wanted = placement == null ? com.example.shop.domain.BannerPlacement.HOME_HERO : placement;
        return bannerService.getActiveBanners().stream().filter(Banner::isTechnologyCatalog).filter(b -> b.getPlacement() == wanted).sorted(java.util.Comparator.comparingLong(Banner::getId).reversed()).map(this::banner).toList();
    }
    private Map<String, Object> category(Category item, long sellable) { return Map.of("id", item.getId(), "name", item.getName(), "image", item.getImage() == null ? "" : item.getImage(), "productCount", sellable); }
    private Map<String, Object> banner(Banner item) { return Map.of("id", item.getId(), "name", item.getName(), "imageUrl", item.getImageUrl() == null ? "" : item.getImageUrl(), "link", item.getLink() == null ? "" : item.getLink(), "placement", item.getPlacement()); }
}
