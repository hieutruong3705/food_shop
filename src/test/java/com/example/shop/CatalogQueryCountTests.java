package com.example.shop;

import com.example.shop.domain.*;
import com.example.shop.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.jpa.properties.hibernate.generate_statistics=true","logging.level.org.hibernate.stat=OFF","logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogQueryCountTests {
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory factory;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired PromotionRepository promotions;
    @Autowired MockMvc mvc;

    @Test
    void measureTwelveProductPage() throws Exception {
        var b=new Brand(); b.setName("Query brand"); brands.save(b);
        var c=new Category(); c.setName("Query category"); c.setBrands(new ArrayList<>(List.of(b))); categories.save(c);
        var rows=new ArrayList<Product>();
        for(int i=0;i<12;i++) {
            var p=new Product(); p.setName("Query product "+i); p.setBrand(b); p.setCategory(c); p.setPrice(1000000); p.setQuantity(5);
            var image=new ProductImage(); image.setProduct(p); image.setImageUrl("/tech/laptop.svg"); p.getImages().add(image);
            var spec=new ProductSpec(); spec.setProduct(p); spec.setSpecName("RAM"); spec.setSpecValue("16GB"); p.getSpecs().add(spec);
            products.save(p); rows.add(p);
        }
        var offer=new Promotion(); offer.setName("Query offer"); offer.setScopeType(ScopeType.PRODUCT);
        offer.setProducts(rows); offer.setDiscountRate(10); offer.setStartDate(new Date(System.currentTimeMillis()-60000)); offer.setEndDate(new Date(System.currentTimeMillis()+600000)); promotions.save(offer);
        em.flush(); em.clear();
        var stats=factory.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        mvc.perform(get("/api/v1/products").param("size","12")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(12));
        System.out.println("CATALOG_QUERY_COUNT="+stats.getPrepareStatementCount());
    }
}
