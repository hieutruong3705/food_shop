package com.example.shop;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AdminApiTests {
    @Test void stoppedProductsCanResumeWithoutChangingStockAndSoldOutSortsLastAcrossPages() throws Exception {
        String key=UUID.randomUUID().toString();
        long category=create("categories",Map.of("name",key));long brand=create("brands",Map.of("name",key));
        var ids=new ArrayList<Long>();
        for(int i=0;i<4;i++) {
            var body=new HashMap<String,Object>();body.put("name",key+i);body.put("sku",key+i);body.put("slug",key+i);body.put("warrantyMonths",12);
            body.put("categoryId",category);body.put("brandId",brand);body.put("price",1000000*(i+1));body.put("quantity",i==0||i==3?0:2);
            ids.add(create("products",body));
        }
        for(String sort:List.of("newest","popular","price-asc","price-desc")) {
            for(int page=0;page<2;page++) {
                var response=mvc.perform(get("/api/v1/products").param("categoryId",Long.toString(category)).param("sort",sort).param("size","2").param("page",Integer.toString(page)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4)).andReturn();
                var rows=json.readTree(response.getResponse().getContentAsString()).get("content");
                for(var row:rows) assertThat(row.get("quantity").asLong()).isEqualTo(page==0?2:0);
                if(sort.startsWith("price")) assertThat(rows.get(0).get("price").asDouble()<rows.get(1).get("price").asDouble()).isEqualTo(sort.equals("price-asc"));
            }
        }
        long id=ids.get(1);
        mvc.perform(get("/api/v1/admin/products").with(user(email).roles("ADMIN")).param("categoryId",Long.toString(category)).param("size","2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].quantity").value(2)).andExpect(jsonPath("$.content[1].quantity").value(2));
        mvc.perform(get("/api/v1/admin/products").with(user(email).roles("ADMIN")).param("categoryId",Long.toString(category)).param("size","2").param("page","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].quantity").value(0)).andExpect(jsonPath("$.content[1].quantity").value(0));
        mvc.perform(patch("/api/v1/admin/products/"+id+"/status").with(csrf()).contentType("application/json").content("{\"active\":true}")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/admin/products/"+id+"/status").with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"active\":\"true\"}")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/admin/products/"+id).with(user(email).roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/products/"+id)).andExpect(status().isNotFound());
        for(int retry=0;retry<2;retry++) mvc.perform(patch("/api/v1/admin/products/"+id+"/status").with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"active\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true)).andExpect(jsonPath("$.quantity").value(2));
        mvc.perform(get("/api/v1/products/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.product.quantity").value(2));
        mvc.perform(patch("/api/v1/admin/products/"+ids.get(0)+"/status").with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"active\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(0));
    }
    @Test void categoryFacetsAndCombinedFiltersUsePersistedCatalog() throws Exception {
        String key=UUID.randomUUID().toString();
        long category=create("categories",Map.of("name",key,"image","/tech/monitor.svg"));
        long other=create("categories",Map.of("name",key+"other"));
        long brand=create("brands",Map.of("name",key,"logoUrl","/tech/laptop.svg"));
        for(int i=0;i<5;i++) {
            var body=new HashMap<String,Object>();body.put("name",key+i);body.put("sku",key+i);body.put("slug",key+i);body.put("warrantyMonths",12);
            body.put("categoryId",i==4?other:category);body.put("brandId",brand);body.put("price",1000000+i*1000000);body.put("quantity",i==2?0:3);body.put("active",i!=3);
            body.put("specs",List.of(Map.of("name","RAM","value",i==0?"8 GB":"16 GB"),Map.of("name","CPU","value",i==1?"Core i7":"Core i5")));
            create("products",body);
        }
        mvc.perform(get("/api/v1/catalog/facets").param("categoryId",Long.toString(category)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.inStockCount").value(2))
            .andExpect(jsonPath("$.brands[0].logoUrl").value("/tech/laptop.svg"))
            .andExpect(jsonPath("$.specs[1].name").value("RAM")).andExpect(jsonPath("$.specs[1].values[0].count").value(2));
        mvc.perform(get("/api/v1/products").param("categoryId",Long.toString(category)).param("brandIds",Long.toString(brand)).param("spec.RAM","8 GB","16 GB").param("spec.CPU","Core i7").param("inStock","true").param("minPrice","1500000").param("size","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].name").value(key+1));
        mvc.perform(get("/api/v1/products").param("categoryId",Long.toString(category)).param("spec.RAM","32 GB"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/products").param("spec.","bad")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/catalog/facets").param("categoryId","999999999"))
            .andExpect(status().isBadRequest());
    }
    @Test void variantsRoundTripAndValidateOptions()throws Exception{
        String key=UUID.randomUUID().toString();long cat=create("categories",Map.of("name",key));long brand=create("brands",Map.of("name",key));
        var request=new HashMap<String,Object>();request.put("name","Variant test");request.put("sku",key);request.put("slug",key);request.put("price",0);request.put("quantity",0);request.put("warrantyMonths",12);request.put("categoryId",cat);request.put("brandId",brand);
        var v1=new HashMap<String,Object>(Map.of("sku",key+"-B","price",2000000,"quantity",2,"options",Map.of("Màu sắc","Đen","RAM","16GB")));
        var v2=new HashMap<String,Object>(Map.of("sku",key+"-W","price",3000000,"quantity",3,"options",Map.of("Màu sắc","Trắng","RAM","32GB")));
        request.put("variants",List.of(v1,v2));long id=create("products",request);
        var result=mvc.perform(get("/api/v1/products/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.hasVariants").value(true)).andExpect(jsonPath("$.variants.length()").value(2)).andExpect(jsonPath("$.product.quantity").value(5)).andExpect(jsonPath("$.product.price").value(2000000)).andReturn();
        var data=json.readTree(result.getResponse().getContentAsString());v1.put("id",data.get("variants").get(0).get("id").asLong());v2.put("id",data.get("variants").get(1).get("id").asLong());v1.put("active",false);request.put("variants",List.of(v1,v2));
        mvc.perform(put("/api/v1/admin/products/"+id).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/products/"+id)).andExpect(jsonPath("$.variants.length()").value(1)).andExpect(jsonPath("$.product.quantity").value(3)).andExpect(jsonPath("$.product.price").value(3000000));
        for(boolean active:List.of(false,true)) mvc.perform(patch("/api/v1/admin/products/"+id+"/status").with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"active\":"+active+"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(3));
        mvc.perform(get("/api/v1/products/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.variants.length()").value(1)).andExpect(jsonPath("$.product.quantity").value(3));
        v1.put("active",true);v2.put("options",v1.get("options"));
        mvc.perform(put("/api/v1/admin/products/"+id).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
    }
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired UserRepository users;@Autowired RoleRepository roles;@Autowired ProductRepository products;
    String email;
    @BeforeEach void setup(){var role=roles.findByName("ADMIN");if(role==null){role=new Role();role.setName("ADMIN");role=roles.save(role);}var u=new User();email=UUID.randomUUID()+"@admin.test";u.setEmail(email);u.setFullName("Admin");u.setRole(role);u.setEnabled(true);users.save(u);}
    long create(String type,Map<String,Object> data)throws Exception {if(type.equals("products")){long category=((Number)data.get("categoryId")).longValue();var c=json.readTree(mvc.perform(get("/api/v1/admin/categories/"+category).with(user(email).roles("ADMIN"))).andReturn().getResponse().getContentAsString());var values=(com.fasterxml.jackson.databind.node.ArrayNode)c.get("brandIds");long brand=((Number)data.get("brandId")).longValue();if(!java.util.stream.StreamSupport.stream(values.spliterator(),false).anyMatch(x->x.asLong()==brand))values.add(brand);mvc.perform(put("/api/v1/admin/categories/"+category).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(c.toString())).andExpect(status().isOk());}var result=mvc.perform(post("/api/v1/admin/"+type).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isCreated()).andReturn();long id=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();String location="/api/v1/admin/"+type+"/"+id;assertThat(result.getResponse().getHeader("Location")).isEqualTo(location);mvc.perform(get(location).with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));mvc.perform(get(location)).andExpect(status().isUnauthorized());return id;}
    @Test void productCrudAndFilteringPreserveRelationships()throws Exception{
        String key=UUID.randomUUID().toString();long category=create("categories",Map.of("name","Category "+key));long brand=create("brands",Map.of("name","Brand "+key));
        var body=new HashMap<String,Object>();body.put("name","Laptop "+key);body.put("sku",key);body.put("slug",key);body.put("categoryId",category);body.put("brandId",brand);body.put("price",1000000);body.put("quantity",3);body.put("warrantyMonths",12);body.put("images",List.of("/tech/laptop.svg"));body.put("specs",List.of(Map.of("name","RAM","value","16GB")));
        long id=create("products",body);mvc.perform(get("/api/v1/products").param("search",key).param("categoryId",Long.toString(category))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/products/"+id)).andExpect(jsonPath("$.specifications[0].value").value("16GB"));
        body.put("quantity",5);mvc.perform(put("/api/v1/admin/products/"+id).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/admin/brands/"+brand).with(user(email).roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/admin/products/"+id).with(user(email).roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        assertThat(products.findById(id).orElseThrow().isActive()).isFalse();mvc.perform(get("/api/v1/products/"+id)).andExpect(status().isNotFound());
    }
    @Test void dashboardAndAdminResourcesRespondWithoutEntitiesOrSecrets()throws Exception{
        mvc.perform(get("/api/v1/admin/dashboard").with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.chart").isMap()).andExpect(jsonPath("$.bestSelling").isArray());
        for(String resource:List.of("categories","brands","banners","promotions","vouchers","users","reviews","orders")){
            var result=mvc.perform(get("/api/v1/admin/"+resource).with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain("\"password\"","verificationCode","resetPasswordToken");
        }
        mvc.perform(get("/api/v1/admin/reports/orders").with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=orders.csv"));
        mvc.perform(get("/api/v1/admin/users").param("role","ADMIN").param("locked","false").param("search",email).with(user(email).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/admin/reviews").param("search","Test").param("rating","5").with(user(email).roles("ADMIN"))).andExpect(status().isOk());
    }
    @Test void uploadRejectsHtmlAndAcceptsPng()throws Exception{
        mvc.perform(multipart("/api/v1/admin/uploads").file(new MockMultipartFile("file","attack.png","image/png","<script>alert(1)</script>".getBytes())).with(user(email).roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        mvc.perform(multipart("/api/v1/admin/uploads").file(new MockMultipartFile("file","photo.html","image/png",bytes.toByteArray())).with(user(email).roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/admin/uploads").file(new MockMultipartFile("file","photo.png","text/html",bytes.toByteArray())).with(user(email).roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/admin/uploads").file(new MockMultipartFile("file","photo.png","image/png",bytes.toByteArray())).with(user(email).roles("ADMIN")).with(csrf())).andExpect(status().isCreated()).andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/uploads/")));
    }
    @Test void voucherAdminPersistsRulesAndRejectsInvalidDates()throws Exception{
        var voucher=new HashMap<String,Object>();voucher.put("code","ADMIN-"+UUID.randomUUID());voucher.put("discount",10);voucher.put("all",true);voucher.put("minimumOrder",500000);voucher.put("maximumDiscount",100000);voucher.put("usageLimit",2);voucher.put("active",false);
        long id=create("vouchers",voucher);
        mvc.perform(get("/api/v1/admin/vouchers").param("search",voucher.get("code").toString().toUpperCase(Locale.ROOT)).with(user(email).roles("ADMIN"))).andExpect(jsonPath("$.content[0].minimumOrder").value(500000)).andExpect(jsonPath("$.content[0].usageLimit").value(2)).andExpect(jsonPath("$.content[0].active").value(false));
        voucher.put("startDate","2026-09-20T00:00:00Z");voucher.put("endDate","2026-09-19T00:00:00Z");
        mvc.perform(put("/api/v1/admin/vouchers/"+id).with(user(email).roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(voucher))).andExpect(status().isBadRequest());
    }
    @Test void malformedQueryReturnsVietnameseJson()throws Exception{
        mvc.perform(get("/api/v1/products").param("size","invalid")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Tham số yêu cầu không hợp lệ hoặc còn thiếu")).andExpect(jsonPath("$.trace").doesNotExist());
    }
}
