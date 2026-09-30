package com.example.shop;

import com.example.shop.api.service.PromotionPricing;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OfferScopeTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired PromotionRepository promotions;
    @Autowired VoucherRepository vouchers;
    @Autowired OrderRepository orders;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PromotionPricing pricing;
    String key, email;
    Category c1, c2;
    Brand b1, b2;
    Product p1, p2, p3, inactive;

    @BeforeEach
    void fixture() {
        key = UUID.randomUUID().toString();
        email = key + "@scope.test";
        var role = roles.findByName("ADMIN");
        if (role == null) { role = new Role(); role.setName("ADMIN"); role = roles.save(role); }
        var user = new User(); user.setEmail(email); user.setEnabled(true); user.setRole(role); users.save(user);
        b1 = brand("Dell"); b2 = brand("HP");
        c1 = category("Laptop"); c2 = category("Monitor");
        p1 = product(c1,b1,10); p2 = product(c1,b2,0); p3 = product(c2,b1,5);
        inactive = product(c2,b2,5); inactive.setActive(false);
        em.flush();
    }

    Brand brand(String name) { var b=new Brand(); b.setName(key+name); return brands.save(b); }
    Category category(String name) { var c=new Category(); c.setName(key+name); c.setBrands(new ArrayList<>(List.of(b1,b2))); return categories.save(c); }
    Product product(Category c,Brand b,long stock) {
        var p=new Product(); p.setName(key+" "+c.getName()+" "+b.getName()); p.setSku(UUID.randomUUID().toString());
        p.setCategory(c); p.setBrand(b); p.setPrice(1000000); p.setQuantity(stock); return products.save(p);
    }
    Map<String,Object> scope(String mode) {
        return switch(mode) {
            case "ALL" -> Map.of("scopeType","ALL");
            case "CATEGORY" -> Map.of("scopeType","CATEGORY","categoryIds",List.of(c1.getId()));
            case "BRAND" -> Map.of("scopeType","BRAND","brandIds",List.of(b1.getId()));
            case "CATEGORY_AND_PRODUCT" -> Map.of("scopeType","CATEGORY","categoryIds",List.of(c1.getId()),"productIds",List.of(p3.getId()));
            case "BRAND_AND_PRODUCT" -> Map.of("scopeType","BRAND","brandIds",List.of(b1.getId()),"productIds",List.of(p2.getId()));
            case "PICKED_IN_CATEGORY" -> Map.of("scopeType","CATEGORY","productIds",List.of(p1.getId()));
            case "PICKED_IN_BRAND" -> Map.of("scopeType","BRAND","productIds",List.of(p3.getId()));
            case "MULTIPLE" -> Map.of("scopeType","CATEGORY","categoryIds",List.of(c1.getId(),c2.getId()));
            case "INACTIVE" -> Map.of("scopeType","PRODUCT","productIds",List.of(inactive.getId()));
            default -> Map.of("scopeType","PRODUCT","productIds",List.of(p1.getId(),p2.getId()));
        };
    }
    long create(String resource,Map<String,Object> body) throws Exception {
        var response=mvc.perform(post("/api/v1/admin/"+resource).with(user(email).roles("ADMIN")).with(csrf())
                .contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isCreated()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).path("id").asLong();
    }
    JsonNode getJson(String path) throws Exception {
        return json.readTree(mvc.perform(get(path).with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @ParameterizedTest
    @ValueSource(strings={"ALL","CATEGORY","BRAND","CATEGORY_AND_PRODUCT","BRAND_AND_PRODUCT","PICKED_IN_CATEGORY","PICKED_IN_BRAND","MULTIPLE","PRODUCT","INACTIVE"})
    void allPathsUseTheSameScope(String mode) throws Exception {
        var input = new HashMap<>(scope(mode));
        input.putAll(Map.of("name",key,"discountRate",10,"startDate",Instant.now().minusSeconds(60).toString(),"endDate",Instant.now().plusSeconds(600).toString()));
        long id=create("promotions",input);
        var voucherInput=new HashMap<>(scope(mode)); voucherInput.put("code",key); voucherInput.put("discount",10);
        long vid=create("vouchers",voucherInput);
        em.flush(); em.clear();
        var offer=promotions.findById(id).orElseThrow();
        List<Long> expected=switch(mode) {
            case "ALL","MULTIPLE","CATEGORY_AND_PRODUCT","BRAND_AND_PRODUCT" -> List.of(p1.getId(),p2.getId(),p3.getId());
            case "CATEGORY","PRODUCT" -> List.of(p1.getId(),p2.getId());
            case "BRAND" -> List.of(p1.getId(),p3.getId());
            case "PICKED_IN_CATEGORY" -> List.of(p1.getId());
            case "PICKED_IN_BRAND" -> List.of(p3.getId());
            default -> List.of();
        };
        var fixture=products.findAllById(List.of(p1.getId(),p2.getId(),p3.getId(),inactive.getId()));
        assertThat(fixture.stream().filter(Product::isActive).filter(offer::appliesTo).map(Product::getId)).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(products.findAll(OfferScopeSpecification.visible(offer)).stream().filter(p->p.getName().startsWith(key)).map(Product::getId)).containsExactlyInAnyOrderElementsOf(expected);
        var prices=pricing.snapshot();
        for(var p:fixture) {
            boolean applies=expected.contains(p.getId()) || p.getId()==inactive.getId() && List.of("ALL","MULTIPLE","INACTIVE").contains(mode);
            assertThat(prices.price(p,p.getPrice())).isEqualTo(applies?900000:1000000);
        }
        var result=getJson("/api/v1/products?promotionId="+id+"&size=12&search="+key);
        assertThat(result.path("totalElements").asInt()).isEqualTo(expected.size());
        for(var row:result.path("content")) assertThat(row.path("discountedPrice").asDouble()).isEqualTo(900000);
        var publicOffers=getJson("/api/v1/offers");
        JsonNode found=null;
        for(var row:publicOffers.path("promotions")) if(row.path("id").asLong()==id) found=row;
        assertThat(found).isNotNull();
        assertThat(found.path("hasAvailableProducts").asBoolean()).isEqualTo(!expected.isEmpty());
        for(String kind:List.of("voucher","promotion")) {
            var preview=json.readTree(mvc.perform(post("/api/v1/admin/offers/preview").param("kind",kind).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(scope(mode))))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(preview.path("valid").asBoolean()).isTrue();
            long total=products.count(OfferScopeSpecification.visible(offer));
            long inStock=products.count(OfferScopeSpecification.visible(offer).and((root,q,cb)->cb.gt(root.get("quantity"),0)));
            assertThat(preview.path("total").asLong()).isEqualTo(total);
            assertThat(preview.path("inStock").asLong()).isEqualTo(inStock);
        }
        for(var p:List.of(p1,p3)) mvc.perform(post("/api/v1/cart/items").with(user(email)).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("productId",p.getId(),"quantity",1)))).andExpect(status().isOk());
        long eligible=expected.stream().filter(x->x==p1.getId()||x==p3.getId()).count();
        var quote=mvc.perform(get("/api/v1/cart/quote").param("voucherCode",vouchers.findById(vid).orElseThrow().getCode()).with(user(email)));
        if(eligible==0) quote.andExpect(status().isBadRequest());
        else quote.andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(eligible*90000.0));
    }

    void preview(String kind,String body,boolean valid) throws Exception {
        mvc.perform(post("/api/v1/admin/offers/preview").param("kind",kind).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(valid));
    }

    @Test
    void emptyMixedAndMalformedScopesAreRejected() throws Exception {
        for(var input:List.of(Map.of("scopeType","CATEGORY"),Map.of("scopeType","BRAND"),Map.of("scopeType","PRODUCT"),Map.of("scopeType","CATALOG"),
                Map.<String,Object>of("categoryIds",List.of(c1.getId()),"brandIds",List.of(b1.getId())))) {
            preview("voucher",json.writeValueAsString(input),false);
        }
        preview("promotion","{\"scopeType\":\"PRODUCT\",\"productIds\":[1.5]}",false);
        // The retired CATALOG type still reads as the one group it lists.
        preview("voucher",json.writeValueAsString(Map.of("scopeType","CATALOG","brandIds",List.of(b1.getId()))),true);
        var customerRole=roles.findByName("USER");
        if(customerRole==null) { customerRole=new Role(); customerRole.setName("USER"); customerRole=roles.save(customerRole); }
        var customer=new User(); customer.setEmail(key+"customer@test.local"); customer.setEnabled(true); customer.setRole(customerRole); users.saveAndFlush(customer);
        mvc.perform(post("/api/v1/admin/offers/preview").param("kind","voucher").with(user(customer.getEmail())).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }

    @Test
    void productsAlreadyCoveredByAWholeGroupAreDroppedAndPickedOnesStay() throws Exception {
        long id=create("promotions",Map.of("name",key,"discountRate",10,"scopeType","CATEGORY","categoryIds",List.of(c1.getId()),"productIds",List.of(p1.getId(),p3.getId()),
                "startDate",Instant.now().minusSeconds(60).toString(),"endDate",Instant.now().plusSeconds(600).toString()));
        var saved=getJson("/api/v1/admin/promotions/"+id);
        assertThat(saved.path("scopeType").asText()).isEqualTo("CATEGORY");
        assertThat(saved.path("categoryIds")).extracting(JsonNode::asLong).containsExactly(c1.getId());
        assertThat(saved.path("productIds")).extracting(JsonNode::asLong).containsExactly(p3.getId());
        assertThat(saved.path("scopeDescription").asText()).contains(c1.getName()).contains(p3.getName());
    }

    @Test
    void pickersShowPicturesCountsAndProductsOfOneGroup() throws Exception {
        var groups=getJson("/api/v1/admin/options/categories?search="+key).path("content");
        assertThat(groups).hasSize(2);
        for(var row:groups) assertThat(row.path("productCount").asLong()).isEqualTo(row.path("id").asLong()==c1.getId()?2:1);
        var brandRows=getJson("/api/v1/admin/options/brands?search="+key).path("content");
        for(var row:brandRows) assertThat(row.path("productCount").asLong()).isEqualTo(row.path("id").asLong()==b1.getId()?2:1);
        assertThat(getJson("/api/v1/admin/options/products?search="+key+"&categoryId="+c1.getId()).path("content")).extracting(row->row.path("id").asLong())
                .containsExactlyInAnyOrder(p1.getId(),p2.getId());
        var byBrand=getJson("/api/v1/admin/options/products?search="+key+"&brandId="+b1.getId()).path("content");
        assertThat(byBrand).extracting(row->row.path("id").asLong()).containsExactlyInAnyOrder(p1.getId(),p3.getId());
        for(var row:byBrand) assertThat(row.path("brandId").asLong()).isEqualTo(b1.getId());
    }

    @Test
    void voucherCategoryFilterAlsoFindsVouchersForSingleProductsOfThatCategory() throws Exception {
        String code=key.substring(0,8).toUpperCase(Locale.ROOT);
        create("vouchers",Map.of("code",code+"-WHOLE","discount",10,"scopeType","CATEGORY","categoryIds",List.of(c1.getId())));
        create("vouchers",Map.of("code",code+"-PICKED","discount",10,"scopeType","CATEGORY","productIds",List.of(p1.getId())));
        create("vouchers",Map.of("code",code+"-BRANDPICK","discount",10,"scopeType","BRAND","productIds",List.of(p2.getId())));
        create("vouchers",Map.of("code",code+"-OTHER","discount",10,"scopeType","PRODUCT","productIds",List.of(p3.getId())));
        create("vouchers",Map.of("code",code+"-ALL","discount",10,"scopeType","ALL"));
        em.flush();
        var rows=getJson("/api/v1/admin/vouchers?size=50&search="+code+"&categoryId="+c1.getId()).path("content");
        assertThat(rows).extracting(row->row.path("code").asText())
                .containsExactlyInAnyOrder(code+"-WHOLE",code+"-PICKED",code+"-BRANDPICK",code+"-ALL");
    }

    @Test
    void runningPromotionSetsTheSaleTagOnProducts() throws Exception {
        create("promotions",Map.of("name","Tag "+key,"discountRate",15,"scopeType","PRODUCT","productIds",List.of(p1.getId()),
                "startDate",Instant.now().minusSeconds(60).toString(),"endDate",Instant.now().plusSeconds(600).toString()));
        var tagged=getJson("/api/v1/products/"+p1.getId()).path("product");
        assertThat(tagged.path("discountRate").asDouble()).isEqualTo(15);
        assertThat(tagged.path("promotionName").asText()).isEqualTo("Tag "+key);
        assertThat(tagged.path("promotionEndsAt").isNull()).isFalse();
        var plain=getJson("/api/v1/products/"+p2.getId()).path("product");
        assertThat(plain.path("discountRate").asDouble()).isZero();
        assertThat(plain.path("promotionName").isNull()).isTrue();
    }

    @Test
    void deletionNamesBlockingOffersAndAssignedCategories() throws Exception {
        var empty=category("Empty");
        create("vouchers",Map.of("code","TECH-"+key,"discount",10,"scopeType","CATEGORY","categoryIds",List.of(empty.getId())));
        create("promotions",Map.of("name","Sale "+key,"discountRate",10,"scopeType","CATEGORY","categoryIds",List.of(empty.getId()),"startDate",Instant.now().toString(),"endDate",Instant.now().plusSeconds(600).toString()));
        mvc.perform(delete("/api/v1/admin/categories/"+empty.getId()).with(user(email).roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TECH-"+key.toUpperCase())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sale "+key)));
        var assigned=brand("Assigned"); empty.getBrands().add(assigned); em.flush();
        mvc.perform(delete("/api/v1/admin/brands/"+assigned.getId()).with(user(email).roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(empty.getName())));
    }

    @Test
    void skuChecksAcrossTablesAreCaseInsensitiveAndExcludeEditedRecord() {
        var service=new com.example.shop.api.service.SkuService(products,variants);
        var variant=new ProductVariant(); variant.setProduct(p1); variant.setSku("Sku-"+key); variant.setPrice(100); variant.setQuantity(1);
        variants.saveAndFlush(variant);
        assertThatThrownBy(()->service.checkProduct(variant.getSku().toUpperCase(),p2.getId())).hasMessageContaining(p1.getName()).hasMessageContaining("SKU");
        assertThatThrownBy(()->service.checkVariant(p2.getSku().toUpperCase(),variant.getId())).hasMessageContaining(p2.getName());
        assertThatCode(()->service.checkProduct(p1.getSku().toUpperCase(),p1.getId())).doesNotThrowAnyException();
        assertThatCode(()->service.checkVariant(variant.getSku().toUpperCase(),variant.getId())).doesNotThrowAnyException();
    }

    @Test
    void revenueUsesCompletionDateWhileCountersUseCreationDate() throws Exception {
        var zone=java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        var day=java.time.LocalDate.of(2024,2,10);
        Date now=Date.from(day.atStartOfDay(zone).plusHours(12).toInstant()), earlier=Date.from(day.minusDays(10).atStartOfDay(zone).toInstant());
        for(int i=0;i<4;i++) {
            var o=new Order(); o.setStatus(i==3?OrderStatus.CANCELLED:OrderStatus.COMPLETED); o.setTotalPrice(100+i); orders.saveAndFlush(o);
            o.setCreatedAt(i==0?earlier:now); o.setCompletedAt(i==0?now:i==1?earlier:null);
        }
        em.flush();
        var dashboard=getJson("/api/v1/admin/dashboard?startDate=2024-02-10&endDate=2024-02-10");
        assertThat(dashboard.path("revenue").asDouble()).isEqualTo(202);
        assertThat(dashboard.path("newOrders").asInt()).isEqualTo(2);
        assertThat(dashboard.path("cancelledOrders").asInt()).isEqualTo(1);
        assertThat(dashboard.path("completedOrders").asInt()).isEqualTo(2);
    }
}
