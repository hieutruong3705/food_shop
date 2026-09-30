package com.example.shop;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ScopeApiTests {
    @Autowired com.example.shop.api.service.PromotionPricing pricing;
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired CategoryRepository categories;@Autowired BrandRepository brands;@Autowired ProductRepository products;@Autowired UserRepository users;@Autowired RoleRepository roles;@Autowired PromotionRepository promotions;@Autowired VoucherRepository vouchers;@Autowired EntityManager em;
    String email;String catalogKey=UUID.randomUUID().toString();Category c1,c2,c3;Brand b1,b2;Product p1,p2,p3;
    @BeforeEach void setup(){
        var role=roles.findByName("ADMIN");if(role==null){role=new Role();role.setName("ADMIN");role=roles.save(role);}var u=new User();email=UUID.randomUUID()+"@scope.test";u.setEmail(email);u.setEnabled(true);u.setRole(role);users.save(u);
        b1=brand();b2=brand();c1=category(b1);c2=category(b2);c3=category(b1);
        p1=product(c1,b1,1000000,12);p2=product(c2,b2,2000000,24);p3=product(c3,b1,3000000,36);em.flush();em.clear();
    }
    Brand brand(){var b=new Brand();b.setName(UUID.randomUUID().toString());return brands.save(b);}
    Category category(Brand b){var c=new Category();c.setName(UUID.randomUUID().toString());c.setBrands(new ArrayList<>(List.of(b)));return categories.save(c);}
    Product product(Category c,Brand b,double price,int warranty){var p=new Product();p.setName(catalogKey+UUID.randomUUID());p.setCategory(c);p.setBrand(b);p.setPrice(price);p.setQuantity(10);p.setWarrantyMonths(warranty);return products.save(p);}
    /** Every list is sent; only the ones that belong to the type count, and p1 is already covered by CATEGORY (c1) and BRAND (b1). */
    Map<String,Object> scope(String type){var n=new HashMap<String,Object>();n.put("scopeType",type);n.put("categoryIds",List.of(c1.getId(),c2.getId()));n.put("brandIds",List.of(b1.getId()));n.put("productIds",type.equals("PRODUCT")?List.of(p1.getId(),p2.getId()):List.of(p1.getId()));return n;}
    long create(String resource,Map<String,Object> n)throws Exception{return json.readTree(mvc.perform(post("/api/v1/admin/"+resource).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(n))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();}
    @Test void promotionsPriceAndListingRespectEveryScopeAndNormalizeLists()throws Exception{
        for(String scope:List.of("ALL","CATEGORY","BRAND","PRODUCT")){
            var n=scope(scope);n.put("name","scope-test");n.put("discountRate",10);n.put("startDate",java.time.Instant.now().minusSeconds(3600).toString());n.put("endDate",java.time.Instant.now().plusSeconds(3600).toString());
            long id=create("promotions",n);em.flush();em.clear();
            assertThat(pricing.snapshot().price(products.findById(p1.getId()).orElseThrow(), products.findById(p1.getId()).orElseThrow().getPrice())).isEqualTo(900000);
            boolean third=scope.equals("ALL")||scope.equals("BRAND");
            assertThat(pricing.snapshot().price(products.findById(p3.getId()).orElseThrow(), products.findById(p3.getId()).orElseThrow().getPrice())).isEqualTo(third?2700000:3000000);
            mvc.perform(get("/api/v1/products").param("promotionId",""+id).param("search",catalogKey)).andExpect(jsonPath("$.totalElements").value(scope.equals("ALL")?3:2));
            var offer=promotions.findById(id).orElseThrow();assertThat(offer.getCategories().isEmpty()).isEqualTo(!scope.equals("CATEGORY"));assertThat(offer.getBrands().isEmpty()).isEqualTo(!scope.equals("BRAND"));assertThat(offer.getProducts().isEmpty()).isEqualTo(!scope.equals("PRODUCT"));
            promotions.delete(offer);em.flush();em.clear();
        }
    }
    @Test void voucherQuoteUsesOnlyEligibleLinesForEachScope()throws Exception{
        for(var p:List.of(p1,p2,p3))mvc.perform(post("/api/v1/cart/items").with(user(email)).with(csrf()).contentType("application/json").content("{\"productId\":"+p.getId()+",\"quantity\":1}")).andExpect(status().isOk());
        for(String type:List.of("ALL","CATEGORY","BRAND","PRODUCT")){
            var n=scope(type);String code="SCOPE-"+UUID.randomUUID();n.put("code",code);n.put("discount",10);create("vouchers",n);em.flush();em.clear();
            mvc.perform(get("/api/v1/cart/quote").param("voucherCode",code).with(user(email))).andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(type.equals("ALL")?600000.0:type.equals("BRAND")?400000.0:300000.0));
        }
    }
    @Test void scopesRejectEmptyUnknownIdsAndWrongCategoryBrand()throws Exception{
        for(String type:List.of("CATEGORY","BRAND","PRODUCT"))for(String module:List.of("promotions","vouchers")){
            var n=new HashMap<String,Object>(Map.of("name","invalid","code","invalid","discount",10,"discountRate",10,"scopeType",type,"startDate",java.time.Instant.now().minusSeconds(100).toString(),"endDate",java.time.Instant.now().plusSeconds(100).toString()));
            mvc.perform(post("/api/v1/admin/"+module).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(n))).andExpect(status().isBadRequest());
        }
        var n=new HashMap<String,Object>(Map.of("name","invalid","sku","invalid","slug","invalid","price",100,"quantity",1,"warrantyMonths",12,"categoryId",c1.getId(),"brandId",b2.getId()));
        mvc.perform(post("/api/v1/admin/products").with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(n))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/admin/categories/"+c1.getId()).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("name",c1.getName(),"brandIds",List.of())))).andExpect(status().isBadRequest());
    }
    @Test void assignedBrandsIncludeEmptyBrandAndWarrantyRangeUsesNumericField()throws Exception{
        var c=categories.findById(c1.getId()).orElseThrow();c.getBrands().add(brands.findById(b2.getId()).orElseThrow());em.flush();em.clear();
        mvc.perform(get("/api/v1/catalog/menu").param("categoryId",""+c1.getId())).andExpect(jsonPath("$.brands.length()").value(2)).andExpect(jsonPath("$.minWarranty").value(12)).andExpect(jsonPath("$.maxWarranty").value(12));
        mvc.perform(get("/api/v1/products").param("search",catalogKey).param("minWarranty","24").param("maxWarranty","36")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/products").param("minWarranty","36").param("maxWarranty","12")).andExpect(status().isBadRequest());
    }
}
