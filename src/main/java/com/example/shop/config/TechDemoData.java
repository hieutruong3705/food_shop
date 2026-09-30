package com.example.shop.config;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
@Component @Profile("dev") @ConditionalOnProperty(name="shop.demo.enabled",havingValue="true")
public class TechDemoData implements CommandLineRunner {
    @org.springframework.beans.factory.annotation.Autowired private BannerRepository banners;
    @org.springframework.beans.factory.annotation.Autowired private PromotionRepository promotions;
    private final ProductRepository products;private final CategoryRepository categories;private final BrandRepository brands;private final RoleRepository roles;private final UserRepository users;private final PasswordEncoder encoder;private final PolicyRepository policies;private final VoucherRepository vouchers;
    public TechDemoData(ProductRepository p,CategoryRepository c,BrandRepository b,RoleRepository r,UserRepository u,PasswordEncoder e,PolicyRepository po,VoucherRepository v){products=p;categories=c;brands=b;roles=r;users=u;encoder=e;policies=po;vouchers=v;}
    @Override @Transactional public void run(String... args) {
        for(String roleName:new String[]{"ADMIN","USER"}) {
            Role role=roles.findByName(roleName);if(role==null){role=new Role();role.setName(roleName);role=roles.save(role);}
            String email=roleName.toLowerCase()+"@techshop.test";
            String password=System.getenv("DEMO_"+roleName+"_PASSWORD");
            if(password!=null && password.length()>=8 && !users.existsByEmail(email)){var u=new User();u.setEmail(email);u.setFullName("TechShop "+roleName);u.setPassword(encoder.encode(password));u.setRole(role);u.setEnabled(true);users.save(u);}
        }
        String[] names={"Laptop","Điện thoại","Máy tính bảng","Màn hình","Bàn phím","Chuột","Tai nghe","Phụ kiện"};
        String[] titles={"Laptop NovaBook","Điện thoại Nova Phone","Máy tính bảng Nova Pad","Màn hình Nova Display","Bàn phím Nova Keys","Chuột Nova Mouse","Tai nghe Nova Sound","Hub USB-C Nova Connect"};
        String[] icons={"laptop","phone","tablet","monitor","keyboard","mouse","headphones","accessory"};
        long[] prices={15990000,7990000,5990000,3290000,890000,490000,1290000,690000};
        String[] pitch={"Mỏng nhẹ, pin cả ngày, đủ mạnh cho học tập và làm việc.","Màn hình sáng, camera rõ nét, dùng mượt cả ngày.","Màn hình lớn để xem phim, đọc sách và ghi chú.","Hiển thị sắc nét, viền mỏng, dễ đặt cạnh laptop.","Gõ êm, hành trình phím ngắn, kết nối ổn định.","Ôm tay, cảm biến chính xác, dùng lâu không mỏi.","Âm thanh rõ, đeo êm, chống ồn khi cần tập trung.","Mở rộng cổng kết nối cho laptop và điện thoại chỉ với một sợi cáp."};
        Brand brand=brands.findByNameContainingIgnoreCase("Nova").stream().filter(b -> b.getName().equals("Nova")).findFirst().orElseGet(() -> {var b=new Brand();b.setName("Nova");b.setDescription("Thương hiệu giả lập dành cho dữ liệu demo");return brands.save(b);});
        for(int i=0;i<names.length;i++){
            final String name=names[i];Category cat=categories.findByNameContainingIgnoreCase(name).stream().filter(c -> c.getName().equals(name)).findFirst().orElseGet(() -> {var c=new Category();c.setName(name);return categories.save(c);});
            for(int j=1;j<=4;j++){
                String sku="TECH-DEMO-"+i+"-"+j;if(products.existsBySku(sku))continue;
                var p=new Product();p.setName(titles[i]+" "+j);p.setSku(sku);p.setSlug("nova-"+icons[i]+"-"+j);p.setPrice(prices[i]+(j-1)*100000);p.setQuantity(j==4?0:20+j);p.setCategory(cat);p.setBrand(brand);p.setWarrantyMonths(24);p.setShortDesc(pitch[i]);p.setDetailDesc("Sản phẩm minh họa cho cửa hàng TechShop. Thông số và giá là dữ liệu demo, không đại diện sản phẩm thương mại.");
                var image=new ProductImage();image.setProduct(p);image.setImageUrl("/tech/"+icons[i]+".svg");p.getImages().add(image);
                String[][] specs={{"Kết nối","USB-C / Bluetooth"},{"Bảo hành","24 tháng"},{"Phiên bản","Demo "+j}};
                for(var pair:specs){var s=new ProductSpec();s.setProduct(p);s.setSpecName(pair[0]);s.setSpecValue(pair[1]);p.getSpecs().add(s);}ensureBrand(p);products.save(p);
            }
        }
        if(policies.count()==0){for(String name:new String[]{"Chính sách bảo hành","Giao hàng và đổi trả","Bảo mật thông tin"}){var p=new Policy();p.setName(name);p.setActive(true);p.setContent("TechShop hỗ trợ khách hàng theo điều kiện bảo hành của từng sản phẩm. Liên hệ cửa hàng để được hướng dẫn. Nội dung mẫu dành cho môi trường phát triển.");policies.save(p);}}
        if(vouchers.findByCode("TECH10")==null){var v=new Voucher();v.setCode("TECH10");v.setDiscount(10);v.setAll(true);vouchers.save(v);}
        Brand orbit=brands.findByNameContainingIgnoreCase("Orbit Demo").stream().filter(b->b.getName().equals("Orbit Demo")).findFirst().orElseGet(()->{var b=new Brand();b.setName("Orbit Demo");b.setDescription("Thương hiệu minh họa thứ hai");return brands.save(b);});
        var accessory=categories.findByNameContainingIgnoreCase("Phụ kiện").stream().filter(c->c.getName().equals("Phụ kiện")).findFirst().orElseThrow();
        for(int index=1;index<=2;index++){
            String sku="TECH-DEMO-ORBIT-"+index;if(products.existsBySku(sku))continue;
            var p=new Product();p.setName("Hub USB-C Orbit Demo "+index);p.setSku(sku);p.setSlug("orbit-hub-demo-"+index);p.setBrand(orbit);p.setCategory(accessory);p.setPrice(390000+index*100000);p.setQuantity(15);p.setWarrantyMonths(12);p.setShortDesc("Phụ kiện kết nối minh họa TechShop");p.setDetailDesc("Dữ liệu demo, không đại diện sản phẩm thương mại.");
            var image=new ProductImage();image.setProduct(p);image.setImageUrl("/tech/accessory.svg");p.getImages().add(image);ensureBrand(p);products.save(p);
        }
        if(promotions.findAll().stream().noneMatch(p->"TECH-DEMO-ORBIT".equals(p.getName()))){
            var promotion=new Promotion();promotion.setName("TECH-DEMO-ORBIT");promotion.setDescription("Giảm 5% phụ kiện Orbit Demo");promotion.setDiscountRate(5);promotion.setStartDate(new java.util.Date());promotion.setEndDate(java.util.Date.from(java.time.Instant.now().plus(365,java.time.temporal.ChronoUnit.DAYS)));promotion.setScopeType(ScopeType.BRAND);promotion.setBrands(java.util.List.of(orbit));promotion.setProducts(java.util.List.of());promotions.save(promotion);
        }
        if(banners.findAll().stream().noneMatch(b->"TechShop Demo - Phụ kiện".equals(b.getName()))){var b=new Banner();b.setName("TechShop Demo - Phụ kiện");b.setImageUrl("/tech/accessory.svg");b.setLink("/products?brandId="+orbit.getId());b.setActive(true);b.setTechnologyCatalog(true);banners.save(b);}
        if(!products.existsBySku("TECH-DEMO-VARIANTS")){
            var p=new Product();p.setName("Điện thoại Orbit nhiều phiên bản");p.setSku("TECH-DEMO-VARIANTS");p.setSlug("orbit-nhieu-phien-ban");p.setBrand(orbit);p.setCategory(categories.findByNameContainingIgnoreCase("Điện thoại").stream().filter(c->c.getName().equals("Điện thoại")).findFirst().orElseThrow());p.setWarrantyMonths(12);p.setShortDesc("Chọn màu sắc và dung lượng phù hợp");p.setDetailDesc("Sản phẩm demo để thử lựa chọn phiên bản. Mỗi cấu hình có SKU, giá và tồn kho riêng.");
            var image=new ProductImage();image.setProduct(p);image.setImageUrl("/tech/phone.svg");p.getImages().add(image);
            String[] colors={"Đen","Trắng"};String[] storage={"128 GB","256 GB"};int index=0;
            for(String color:colors)for(String capacity:storage){var v=new ProductVariant();v.setProduct(p);v.setSku("TECH-DEMO-VARIANT-"+(++index));v.setPrice(capacity.equals("128 GB")?5990000:6990000);v.setQuantity(index==4?0:10);v.setImageUrl("/tech/phone.svg");v.getOptions().put("Màu sắc",color);v.getOptions().put("Dung lượng",capacity);p.getVariants().add(v);}
            p.setPrice(5990000);p.setQuantity(30);ensureBrand(p);products.save(p);
        }
    }
    private void ensureBrand(Product p){if(p.getCategory().getBrands().stream().noneMatch(b->b.getId()==p.getBrand().getId()))p.getCategory().getBrands().add(p.getBrand());}
}
