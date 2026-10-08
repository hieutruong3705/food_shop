package com.example.shop;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** The banner image an admin uploads for an offer, and the placement that keeps voucher banners off the home page. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class OfferImageAndBannerPlacementTests {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired UserRepository users;@Autowired RoleRepository roles;
    @Autowired BannerRepository banners;@Autowired PromotionRepository promotions;@Autowired VoucherRepository vouchers;
    @Autowired ProductRepository products;@Autowired CategoryRepository categories;@Autowired BrandRepository brands;
    String email;
    @BeforeEach void setup(){var role=roles.findByName("ADMIN");if(role==null){role=new Role();role.setName("ADMIN");role=roles.save(role);}var u=new User();email=UUID.randomUUID()+"@admin.test";u.setEmail(email);u.setFullName("Admin");u.setRole(role);u.setEnabled(true);users.save(u);}
    Product product(){var c=new Category();c.setName(UUID.randomUUID().toString());c=categories.save(c);var b=new Brand();b.setName(UUID.randomUUID().toString());var p=new Product();p.setName(UUID.randomUUID().toString());p.setCategory(c);p.setBrand(brands.save(b));p.setPrice(1000000);p.setQuantity(2);p.setTechnologyCatalog(true);return products.save(p);}
    Banner banner(BannerPlacement placement){var b=new Banner();b.setName(UUID.randomUUID().toString());b.setImageUrl("/tech/laptop.svg");b.setLink("/products");b.setActive(true);b.setTechnologyCatalog(true);b.setPlacement(placement);return banners.save(b);}

    @Test void offersCarryTheUploadedImageAndAnEmptyStringWhenThereIsNone()throws Exception{
        var p=product();
        var withImage=new Promotion();withImage.setScopeType(ScopeType.PRODUCT);withImage.setName("Banner offer "+UUID.randomUUID());withImage.setDiscountRate(9);withImage.setImageUrl("/uploads/promo.png");
        withImage.setStartDate(new Date(System.currentTimeMillis()-10000));withImage.setEndDate(new Date(System.currentTimeMillis()+1000000));withImage.setProducts(List.of(p));withImage.setBrands(List.of());promotions.save(withImage);
        var coded=new Voucher();coded.setCode("IMG-"+UUID.randomUUID());coded.setDiscount(12);coded.setAll(true);coded.setImageUrl("/uploads/voucher.png");vouchers.save(coded);
        var plain=new Voucher();plain.setCode("PLAIN-"+UUID.randomUUID());plain.setDiscount(11);plain.setAll(true);vouchers.save(plain);

        var body=json.readTree(mvc.perform(get("/api/v1/offers")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var promotion=find(body.get("promotions"),"name",withImage.getName());
        Assertions.assertEquals("/uploads/promo.png",promotion.get("imageUrl").asText());
        Assertions.assertEquals("/uploads/voucher.png",find(body.get("vouchers"),"code",coded.getCode()).get("imageUrl").asText());
        // A voucher without an image still carries the key, as an empty string, so the storefront never reads null.
        Assertions.assertEquals("",find(body.get("vouchers"),"code",plain.getCode()).get("imageUrl").asText());

        mvc.perform(get("/api/v1/home")).andExpect(status().isOk()).andExpect(jsonPath("$.promotion.imageUrl").exists());
    }

    @Test void bannersAnswerTheHomeHeroUnlessAnotherPlacementIsAsked()throws Exception{
        var home=banner(BannerPlacement.HOME_HERO);
        var voucherPage=banner(BannerPlacement.VOUCHER_PAGE);
        var banner1=banner(BannerPlacement.HOME_BANNER_1);
        var banner2=banner(BannerPlacement.HOME_BANNER_2);
        var banner3=banner(BannerPlacement.HOME_BANNER_3);
        var brand11=banner(BannerPlacement.HOME_BRAND_1_1);

        var defaults=names(mvc.perform(get("/api/v1/banners")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Assertions.assertTrue(defaults.contains(home.getName()));
        Assertions.assertFalse(defaults.contains(voucherPage.getName()));
        Assertions.assertFalse(defaults.contains(banner1.getName()));
        Assertions.assertFalse(defaults.contains(banner2.getName()));
        Assertions.assertFalse(defaults.contains(banner3.getName()));
        Assertions.assertFalse(defaults.contains(brand11.getName()));

        var asked=names(mvc.perform(get("/api/v1/banners").param("placement","VOUCHER_PAGE")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Assertions.assertTrue(asked.contains(voucherPage.getName()));
        Assertions.assertFalse(asked.contains(home.getName()));

        var homeBanner1=names(mvc.perform(get("/api/v1/banners").param("placement","HOME_BANNER_1")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Assertions.assertTrue(homeBanner1.contains(banner1.getName()));
        Assertions.assertFalse(homeBanner1.contains(home.getName()));

        var homeBanner2=names(mvc.perform(get("/api/v1/banners").param("placement","HOME_BANNER_2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Assertions.assertTrue(homeBanner2.contains(banner2.getName()));
        Assertions.assertFalse(homeBanner2.contains(home.getName()));

        var homeBanner3=names(mvc.perform(get("/api/v1/banners").param("placement","HOME_BANNER_3")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Assertions.assertTrue(homeBanner3.contains(banner3.getName()));
        Assertions.assertFalse(homeBanner3.contains(home.getName()));

        var brandBanner=names(mvc.perform(get("/api/v1/banners").param("placement","HOME_BRAND_1_1")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Assertions.assertTrue(brandBanner.contains(brand11.getName()));
        Assertions.assertFalse(brandBanner.contains(home.getName()));

        mvc.perform(get("/api/v1/banners").param("placement","NOWHERE")).andExpect(status().isBadRequest());
    }

    @Test void adminSavesPlacementAndOfferImagesAndReadsThemBack()throws Exception{
        var banner=new HashMap<String,Object>();banner.put("name","Voucher banner "+UUID.randomUUID());banner.put("imageUrl","/uploads/voucher-banner.png");banner.put("link","/vouchers");banner.put("placement","VOUCHER_PAGE");
        long bannerId=created(post("/api/v1/admin/banners"),banner);
        mvc.perform(get("/api/v1/admin/banners/"+bannerId).with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.placement").value("VOUCHER_PAGE"));

        var homeBanner=new HashMap<String,Object>();homeBanner.put("name","Home banner 1 "+UUID.randomUUID());homeBanner.put("imageUrl","/uploads/home-banner-1.png");homeBanner.put("link","/products?sort=popular");homeBanner.put("placement","HOME_BANNER_1");
        long homeBannerId=created(post("/api/v1/admin/banners"),homeBanner);
        mvc.perform(get("/api/v1/admin/banners/"+homeBannerId).with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.placement").value("HOME_BANNER_1"));

        var homeBanner2=new HashMap<String,Object>();homeBanner2.put("name","Home banner 2 "+UUID.randomUUID());homeBanner2.put("imageUrl","/uploads/home-banner-2.png");homeBanner2.put("link","/products?sort=newest");homeBanner2.put("placement","HOME_BANNER_2");
        long homeBanner2Id=created(post("/api/v1/admin/banners"),homeBanner2);
        mvc.perform(get("/api/v1/admin/banners/"+homeBanner2Id).with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.placement").value("HOME_BANNER_2"));

        var homeBanner3=new HashMap<String,Object>();homeBanner3.put("name","Home banner 3 "+UUID.randomUUID());homeBanner3.put("imageUrl","/uploads/home-banner-3.png");homeBanner3.put("link","/products");homeBanner3.put("placement","HOME_BANNER_3");
        long homeBanner3Id=created(post("/api/v1/admin/banners"),homeBanner3);
        mvc.perform(get("/api/v1/admin/banners/"+homeBanner3Id).with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.placement").value("HOME_BANNER_3"));

        var brandBanner=new HashMap<String,Object>();brandBanner.put("name","Brand 1.1 "+UUID.randomUUID());brandBanner.put("imageUrl","/uploads/brand-1-1.png");brandBanner.put("link","/products");brandBanner.put("placement","HOME_BRAND_1_1");
        long brandBannerId=created(post("/api/v1/admin/banners"),brandBanner);
        mvc.perform(get("/api/v1/admin/banners/"+brandBannerId).with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.placement").value("HOME_BRAND_1_1"));

        // A banner saved without a placement belongs to the home hero, like every banner saved before placements existed.
        var legacy=new HashMap<String,Object>();legacy.put("name","Legacy banner "+UUID.randomUUID());legacy.put("imageUrl","/uploads/legacy.png");
        long legacyId=created(post("/api/v1/admin/banners"),legacy);
        mvc.perform(get("/api/v1/admin/banners/"+legacyId).with(user(email).roles("ADMIN"))).andExpect(jsonPath("$.placement").value("HOME_HERO"));

        var p=product();
        var promotion=new HashMap<String,Object>();promotion.put("name","Promo "+UUID.randomUUID());promotion.put("discountRate",8);promotion.put("imageUrl","/uploads/promo-admin.png");
        promotion.put("startDate",java.time.Instant.now().toString());promotion.put("endDate",java.time.Instant.now().plusSeconds(86400).toString());
        promotion.put("scopeType","PRODUCT");promotion.put("productIds",List.of(p.getId()));
        long promotionId=created(post("/api/v1/admin/promotions"),promotion);
        mvc.perform(get("/api/v1/admin/promotions/"+promotionId).with(user(email).roles("ADMIN"))).andExpect(jsonPath("$.imageUrl").value("/uploads/promo-admin.png"));

        var voucher=new HashMap<String,Object>();voucher.put("code","ADMIN-"+UUID.randomUUID());voucher.put("discount",15);voucher.put("imageUrl","/uploads/voucher-admin.png");voucher.put("scopeType","ALL");
        long voucherId=created(post("/api/v1/admin/vouchers"),voucher);
        mvc.perform(get("/api/v1/admin/vouchers/"+voucherId).with(user(email).roles("ADMIN"))).andExpect(jsonPath("$.imageUrl").value("/uploads/voucher-admin.png"));
    }

    @Test void anOfferImageMustBeAnUploadedPathOrHttps()throws Exception{
        var voucher=new HashMap<String,Object>();voucher.put("code","BAD-"+UUID.randomUUID());voucher.put("discount",15);voucher.put("imageUrl","javascript:alert(1)");voucher.put("scopeType","ALL");
        mvc.perform(post("/api/v1/admin/vouchers").with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(voucher))).andExpect(status().isBadRequest());
    }

    long created(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,Map<String,Object> body)throws Exception{
        var response=mvc.perform(request.with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isCreated()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    }
    com.fasterxml.jackson.databind.JsonNode find(com.fasterxml.jackson.databind.JsonNode rows,String key,String value){
        for(var row:rows) if(value.equals(row.get(key).asText())) return row;
        throw new AssertionError("Không tìm thấy "+key+"="+value);
    }
    List<String> names(String payload)throws Exception{
        var result=new ArrayList<String>();
        for(var row:json.readTree(payload)) result.add(row.get("name").asText());
        return result;
    }
}
