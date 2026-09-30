# Bàn giao cho Codex: hoàn tất P6, P7, P8

Cập nhật 2026-09-30. Nhánh `appmod/java-upgrade-20260917082931`. Commit cuối đã kiểm thử đầy đủ: `c9094b2`.
Nguồn yêu cầu gốc: `data/architecture-review-content.txt` (mục 7, prompt P0–P8). Trạng thái từng phần cũng có trong `docs/ARCHITECTURE-IMPLEMENTATION.md`.

## 1. Trạng thái

| Phần | Trạng thái | Ghi chú |
| --- | --- | --- |
| P0 cấu hình, E2E cô lập, dọn file | Xong, đã commit | `84ac4a6`, `02040aa`, `2356bac`, `7f65e29` |
| P1 IPN trễ, job hết hạn | Xong, đã commit | `7f1f025`, migration V7 |
| P2 nguồn thời gian | Xong, đã commit | `26980b1`. Giá sale vẫn là `@Formula`, xem P7 bên dưới |
| P3 đọc giỏ không khóa, test quy tắc tiền | Xong, đã commit | `e3fb9dc` |
| P4 gửi lại xác minh | Xong, đã commit | `1029026`, migration V8 |
| P5 rate limit, băm token | Xong, đã commit | `c9094b2`, migration V9 |
| **P6** toàn vẹn dữ liệu admin, dashboard | **Làm dở, chưa commit** | Mới có vài truy vấn repository. Còn M3, M4, M5, L3 |
| **Thiết kế lại phạm vi ưu đãi** (voucher và khuyến mãi) | **Làm dở, chưa commit, chưa biên dịch** | Yêu cầu mới của chủ dự án, xem mục 3 |
| **P7** gom quy tắc phạm vi | **Phần lõi đã nằm trong phần làm dở ở trên** | Còn test đặc tả bốn đường và đo số query. Đã gộp vào Prompt A |
| **P8** tách service, enum, code chết, tắt OSIV | **Xong 30/09/2026** | Xem `docs/ARCHITECTURE-IMPLEMENTATION.md`; Prompt B bên dưới chỉ còn giá trị lịch sử |

Trước phần làm dở: backend 81 test đạt (1 bỏ qua) trên H2, Flyway V1–V9 chạy và `ddl-auto=validate` đạt trên MySQL 8. E2E trình duyệt 13/14 (xem mục 5).

## 2. Cảnh báo về cây làm việc

`git status` đang có thay đổi **chưa commit** của phần làm dở. Đừng `git reset`, `git checkout -- .` hay `git stash`. Chúng cần được hoàn tất, không phải bỏ đi. Code chính hiện **chưa biên dịch** (4 lỗi, liệt kê ở mục 4). Test chưa được biên dịch lại nên còn thêm lỗi tham chiếu `ScopeType.CATEGORY`, `Product.getDiscountedPrice()`.

Giữ nguyên kiểu xuống dòng của từng file (nhiều file dùng CRLF). Không sửa migration V1–V9.

> **Đã thay thế (2026-09-30).** Chủ dự án không muốn kết hợp danh mục với hãng. Mô hình `CATALOG` bên dưới đã được thay bằng `ALL / CATEGORY / BRAND / PRODUCT`, mỗi phạm vi là hợp của nhóm chọn cả nhóm và sản phẩm chọn lẻ (xem `docs/ARCHITECTURE-IMPLEMENTATION.md`, migration V11 và `OfferScopeRule`). Phần dưới giữ lại làm lịch sử.

## 3. Thiết kế phạm vi ưu đãi (đã chốt với chủ dự án, đang dở)

Vấn đề cũ: ô chọn chỉ có 4 chế độ loại trừ nhau nên không kết hợp được thương hiệu với danh mục; chọn sản phẩm chỉ hiện chữ, không ảnh; admin không biết ưu đãi sẽ áp dụng cho bao nhiêu sản phẩm; quy tắc bị viết ở 4 nơi (Java, `@Formula` SQL, subquery Criteria, Specification); voucher có thêm cờ `isAll` trùng nghĩa với `scopeType`.

Mô hình mới, `ScopeType { ALL, CATALOG, PRODUCT }`:

| Kiểu | Chọn | Sản phẩm khớp khi |
| --- | --- | --- |
| `ALL` | không chọn gì | luôn khớp |
| `CATALOG` | danh mục và/hoặc thương hiệu | thuộc một trong các danh mục đã chọn (nếu có chọn) **VÀ** thuộc một trong các thương hiệu đã chọn (nếu có chọn). Trong cùng một nhóm là HOẶC |
| `PRODUCT` | sản phẩm | nằm trong danh sách đã chọn |

Ví dụ: chỉ chọn Laptop thì là mọi laptop. Chỉ chọn Dell thì là mọi sản phẩm Dell. Chọn Laptop + Dell + HP thì là laptop của Dell hoặc HP. `CATALOG` không chọn gì thì không khớp sản phẩm nào và API từ chối lưu. Chọn thương hiệu chưa gán cho bất kỳ danh mục nào đã chọn thì API trả 400 kèm tên thương hiệu.

Dữ liệu cũ giữ nguyên nghĩa: `CATEGORY` và `BRAND` cũ thành `CATALOG` chỉ có danh sách danh mục hoặc chỉ có danh sách thương hiệu. Các bảng nối V6 (`promotion_category`, `brand_promotion`, `product_promotion`, `voucher_category`, `voucher_brand`, `voucher_product`) dùng lại nguyên. API vẫn nhận payload cũ (`scopeType` là `CATEGORY` hoặc `BRAND` chỉ đọc danh sách của mình; thiếu `scopeType` thì suy ra từ danh sách nào có dữ liệu; voucher có `all:true`). Ca E2E hiện có tạo khuyến mãi bằng payload chỉ có `productIds`, nên suy luận này phải giữ.

Giao diện mới: ba thẻ chọn (Toàn bộ cửa hàng / Theo danh mục và thương hiệu / Sản phẩm cụ thể) thay cho ô `<select>`; danh sách thương hiệu tự lọc theo danh mục đang chọn; chọn sản phẩm có ảnh thu nhỏ, tên, SKU, giá, hãng, tồn kho; ô xem trước hiện "Áp dụng cho N sản phẩm (M còn hàng)" cùng tối đa 8 ảnh mẫu, và lý do khi lựa chọn không hợp lệ hoặc không khớp sản phẩm nào.

### Đã viết trong cây làm việc (chưa commit)

| File | Nội dung |
| --- | --- |
| `domain/ScopeType.java` | Enum mới kèm `parse()` chấp nhận tên cũ |
| `domain/OfferScopeRule.java` (mới) | `Scope` là nguồn sự thật duy nhất: `matches(...)` cho Java và `predicate(root, cb)` cho Criteria |
| `domain/ScopedOffer.java` | `scope()` và `appliesTo()` chuyển sang dùng quy tắc trên |
| `domain/Voucher.java` | Bỏ field `isAll`. `isAll()` chỉ còn là giá trị suy ra, `setAll()` đặt `ALL` hoặc `CATALOG`. Mặc định `CATALOG` |
| `domain/Product.java` | Bỏ `@Formula activePromotionDiscount`, `getDiscountedPrice`, `discountedPrice`, `isOnSale`, `getDiscountPercentage` |
| `api/service/PromotionPricing.java` (mới) | Tính giá sale bằng Java theo `Clock`: `current()` trả `Discounts` (đọc khuyến mãi đang chạy một lần mỗi request), `Discounts.price(product, listPrice)` |
| `api/service/OfferScopeService.java` (mới) | `apply(offer, payload, isVoucher)` kiểm tra và ghi phạm vi; `preview(payload, isVoucher)` trả tổng số, số còn hàng và tối đa 8 sản phẩm mẫu |
| `repository/OfferScopeSpecification.java` | Dùng `Scope.predicate` |
| `repository/PromotionRepository.java` | `findRunning(now)`, `categoryLinks`, `brandLinks`, `productLinks`, `namesByCategory`, `namesByBrand` |
| `repository/VoucherRepository.java`, `CategoryRepository.java` | `codesByCategory/Brand`, `namesByBrand` (cho M3) |
| `repository/ProductRepository.java`, `ProductVariantRepository.java` | `findAllBySkuIgnoreCase` (cho M4) |
| DTO `ProductSummaryResponse`, `VariantResponse`, `ProductDetailResponse` | Nhận thêm tham số `Discounts` |
| `CatalogApiController`, `ProductApiController`, `CommerceService`, `VariantService` | Dùng `PromotionPricing`. Bộ lọc `promotionId` dùng `scope().predicate` sau khi lấy khuyến mãi đang chạy |
| `OffersApiController` | Hỗ trợ `CATALOG`, thêm khóa `scopeDescription` (một câu tiếng Việt cho trang chủ) |

## 4. Việc còn lại

### Prompt A: hoàn tất P6, thiết kế lại phạm vi ưu đãi và phần lõi P7

```
Bối cảnh: TechShop tại D:\food_shop (Spring Boot 3.5 / Java 17, Vue 3, MySQL 8 + Flyway). Đọc docs/CODEX-HANDOFF.md mục 1–4 trước. Cây làm việc có thay đổi chưa commit của một agent khác, đã xong khoảng 70% việc thiết kế lại phạm vi ưu đãi (voucher và khuyến mãi) và chưa biên dịch được. Hãy hoàn tất, KHÔNG reset/stash/checkout bỏ chúng. Không sửa migration V1–V9. Giữ kiểu xuống dòng từng file.

1. Sửa 4 lỗi biên dịch code chính:
   - AdminCatalogService: xóa hàm saveScope (dùng ScopeType.CATEGORY/BRAND đã bỏ), gọi OfferScopeService.apply(v, n, true) cho voucher và apply(p, n, false) cho khuyến mãi (inject dạng field @Autowired như variantService).
   - AdminCatalogService dòng 42 cột ~577 (hàm product(): tham chiếu getDiscountedPrice/isOnSale đã xóa): dùng PromotionPricing hoặc bỏ khóa đó nếu giao diện admin không dùng.
   - AdminCatalogService bộ lọc voucher theo danh mục: thay root.get("isAll") bằng cb.equal(root.get("scopeType"), ScopeType.ALL).
   - config/TechDemoData.java dòng 48: ScopeType.BRAND -> ScopeType.CATALOG.
2. Migration V10__offer_scope_catalog.sql:
   UPDATE promotions SET scope_type='CATALOG' WHERE scope_type IN ('CATEGORY','BRAND'); tương tự vouchers;
   ALTER TABLE ... ALTER COLUMN scope_type SET DEFAULT 'CATALOG' cho cả hai bảng; ALTER TABLE vouchers DROP COLUMN is_all.
   Cập nhật LegacyMigrationTest (scope_type 'BRAND' -> 'CATALOG'). Chạy nó và toàn bộ test trên MySQL 8 cô lập (xem mục 6 của tài liệu); KHÔNG chạy trên schema techshop của máy.
3. API admin: (a) POST /api/v1/admin/offers/preview?kind=voucher|promotion, body giống payload lưu, gọi OfferScopeService.preview, trả {valid, message?, scopeType, total, inStock, sample:[{id,name,sku,price,image,brand,category,quantity}]}. (b) GET /admin/options/products trả thêm sku, price, image, brand, category, quantity. (c) GET /admin/options/brands nhận thêm categoryIds (danh sách; hãng thuộc BẤT KỲ danh mục nào) ngoài categoryId hiện có.
4. Giao diện (frontend/src): component ScopePicker.vue dùng chung cho voucher và khuyến mãi theo mô tả ở mục 3 tài liệu (3 thẻ chọn, hãng lọc theo danh mục, chọn sản phẩm có ảnh, xem trước debounce 300 ms gọi API ở bước 3a). Thay khối <section class="offer-scope"> và các hàm scopeResource/scopeKey/scopeLabel/scopeSummary/scopeChanged trong views/AdminPage.vue; mở rộng components/SearchMultiSelect.vue (prop categoryIds, hàng có ảnh cho sản phẩm). Dòng tóm tắt trong danh sách: "Danh mục (2) · Thương hiệu (1)". components/HomeHero.vue dòng ~41: hiển thị voucher.scopeDescription. Cập nhật frontend/tests/scopes.spec.js theo giao diện mới.
5. Phần còn lại của P6 (mỗi ý có test, thông báo tiếng Việt nêu tên cụ thể):
   - M3: xóa danh mục đang được voucher/khuyến mãi dùng trả 400 "Danh mục đang được dùng bởi: TECH10, Khuyến mãi X" (voucher theo mã, khuyến mãi theo tên, dùng codesByCategory/namesByCategory). Xóa hãng: kiểm tra thêm sản phẩm, voucher_brand, brand_promotion và category_brands (nêu tên danh mục).
   - M4: SKU sản phẩm và SKU phiên bản phải duy nhất trên cả products.sku lẫn product_variants.sku, không phân biệt hoa thường, loại trừ chính bản ghi đang sửa; lỗi nêu SKU và sản phẩm đang giữ nó. Áp dụng ở VariantService.save và nhánh "products" của AdminCatalogService.save.
   - M5: dashboard tính doanh thu theo completedAt (chỉ đơn COMPLETED, dự phòng coalesce(completedAt, createdAt) cho đơn cũ thiếu ngày). Thêm newOrders (createdAt trong kỳ, trừ CANCELLED), cancelledOrders, completedOrders. Giữ các khóa cũ. Cập nhật lưới số liệu trong AdminPage.vue cho khớp nhãn.
   - L3: xong khi V10 chạy và Voucher không còn field isAll.
6. Phần lõi P7: viết OfferScopeTests đặc tả, với mỗi kiểu ALL/CATALOG (chỉ danh mục, chỉ hãng, cả hai, nhiều mục)/PRODUCT, sản phẩm khớp và không khớp, sản phẩm inactive, so cùng một kết quả giữa Scope.matches, Scope.predicate (OfferScopeSpecification), giá sale (PromotionPricing), /offers hasAvailableProducts, bộ lọc promotionId, tính voucher lúc quote, và endpoint preview. Sửa ScopeApiTests, CommerceApiTests (dòng ~328 dùng ScopeType.CATEGORY) và mọi test tham chiếu Product.getDiscountedPrice. Đo số query của GET /products?size=12 trước (checkout commit c9094b2 vào một git worktree riêng, không dùng stash) và sau, ghi vào docs/ARCHITECTURE-IMPLEMENTATION.md.
7. Điều tra ca E2E lỗi (mục 5 tài liệu).

Tiêu chí xong: .\mvnw.cmd test đạt; test chọn lọc đạt trên MySQL 8 với Flyway V1–V10 và ddl-auto=validate; npm.cmd --prefix frontend run build đạt; .\mvnw.cmd -DskipTests package rồi npm.cmd --prefix frontend run test:e2e đạt 14/14. Commit theo nhóm: (1) P6 M3/M4/M5, (2) backend phạm vi + V10 + test, (3) giao diện phạm vi. Cập nhật bảng trong docs/ARCHITECTURE-IMPLEMENTATION.md. Không dùng --no-verify.
```

### Prompt B: P8

```
Bối cảnh: TechShop tại D:\food_shop. Làm SAU khi Prompt A xong và mọi test đạt. Đọc docs/CODEX-HANDOFF.md và mục P8 trong data/architecture-review-content.txt. Không đổi URL hoặc JSON của API, không sửa migration cũ. Mỗi bước là một commit; .\mvnw.cmd test và npm.cmd --prefix frontend run build đạt sau từng bước. Code mới theo quy ước Java thông thường (một lệnh mỗi dòng, import thay vì tên class đầy đủ, constructor injection).

1. DTO record có Bean Validation cho từng tài nguyên admin (ProductInput, CategoryInput, VoucherInput, PromotionInput...), giữ nguyên tên trường JSON. VoucherInput/PromotionInput mang các trường phạm vi (scopeType, categoryIds, brandIds, productIds, và all cho payload cũ); logic phạm vi đã nằm ở OfferScopeService, chuyển nó sang nhận DTO thay vì JsonNode nhưng giữ nguyên hành vi và thông báo lỗi.
2. Tách AdminCatalogService thành ProductAdminService, CategoryAdminService, OfferAdminService, UserAdminService... mỗi resource có controller riêng (@RequestMapping("/api/v1/admin/products")...). @Transactional chỉ ở service. Nhớ POST /api/v1/admin/offers/preview.
3. enum OrderStatus {PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED} và PaymentStatus {UNPAID, PAID, FAILED, EXPIRED, REFUND_REQUIRED} (REFUND_REQUIRED do P1 thêm), @Enumerated(STRING), không cần migration. Cập nhật OrderTransitions, CommerceService, SandboxPaymentService, PaymentExpiryJob, AdminApiController, JPQL/Specification, DTO. JSON vẫn là chuỗi như cũ.
4. Xóa code chết sau khi xác minh không còn import: giỏ hàng HttpSession trong service/ProductService.java (khoảng dòng 159 trở đi), Promotion.isExpired/isUpcoming nếu không dùng, bộ scaffold TypeScript của frontend (main.ts, App.vue, router/index.ts, stores/*.ts, api/*.ts, layouts/*, views không có hậu tố Page trừ TechHome và AdminPage, vite.config.js/ts trùng) và các script build:typescript, dev:typescript trong frontend/package.json. npm.cmd --prefix frontend run build vẫn đạt.
5. Tắt spring.jpa.open-in-view trong application.properties VÀ xóa dòng spring.jpa.open-in-view=true trong src/test/resources/application-test.properties (nếu không, test không bắt được LazyInitializationException). Sửa lỗi lazy bằng fetch join, @EntityGraph hoặc DTO projection, không bật lại. Chỗ chắc chắn bị ảnh hưởng: ProductApiController.detail (ProductDetailResponse.from đọc images/specs/variants/category ngoài transaction), CatalogApiController.categories()/category() (c.getProducts()), mọi chỗ gọi CommerceResponse.order ngoài @Transactional.

Tiêu chí xong: test đạt sau từng bước, E2E 14/14, bảng trong docs/ARCHITECTURE-IMPLEMENTATION.md cập nhật.
```

## 5. Ca E2E đang lỗi (chưa rõ nguyên nhân)

`frontend/tests/shop.spec.js:282`, "admin creates custom variants, customer selects and cancels with stock restored", quá thời gian 90 giây ở dòng 309 khi bấm nút "Lưu thay đổi" (sau khi điền phiên bản). Ca này đạt ở cuối P0 (14/14) và lỗi ở lần chạy sau P1–P5 (13/14). Chưa xác định do thay đổi nào. Cách làm: `npm.cmd --prefix frontend run build`, `.\mvnw.cmd -DskipTests package`, rồi `cd frontend; npx playwright test tests/shop.spec.js -g "custom variants" --trace on`, mở `npx playwright show-trace`. Xem nút có bị vô hiệu hóa (biến `busy`), có lỗi xác thực form, hoặc có yêu cầu API nào bị treo hay trả 4xx/5xx không (log backend ở `data/e2e-server.log`).

## 6. Ghi chú môi trường

- Windows: dùng `.\mvnw.cmd` và `npm.cmd`. Backend cần JDK 17 trở lên, hiện chạy JDK 21.
- Thư mục `frontend/dist`, `frontend/test-results`, `frontend/playwright-report` từng thuộc tài khoản sandbox của Codex nên tài khoản Admin không xóa được (lỗi EPERM khi build và khi chạy Playwright). Bản cũ đã được đổi tên thành `data/dist-stale-codex`, `frontend/test-results-stale-codex`, `frontend/playwright-report-stale-codex` (đã ignore); có thể xóa sau. Nếu lỗi EPERM lặp lại, đổi tên thư mục đó thay vì xóa.
- Backend dev của chủ dự án đang chạy ở cổng 8080 (profile `dev,mysql-local`, MySQL `techshop`). Không tắt, không chạy migration hay test lên schema đó. E2E chạy H2 ở cổng 18080 (`scripts/run-e2e.mjs`).
- MySQL cô lập để kiểm thử migration: `scripts/start-test-mysql.ps1` hiện không khởi động lại được `data/mysql-test` (lỗi quyền ghi thư mục redo log). Cách đã dùng: `mysqld --no-defaults --initialize-insecure --datadir=<thư mục tạm>`, rồi chạy `mysqld --no-defaults --datadir=<thư mục tạm> --port=3308 --bind-address=127.0.0.1 --mysqlx=0 --default-time-zone=+00:00`, tạo database `techshop_test` (utf8mb4), chạy test với biến môi trường như trong `scripts/test-mysql.ps1` (`SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3308/techshop_test?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh`, user `root` mật khẩu rỗng, `SPRING_FLYWAY_ENABLED=true`, `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`, dialect MySQL). Nhớ tắt sau khi xong.
- Profile `test` và `e2e` tắt rate limit (`shop.rate-limit.enabled=false`); `RateLimitTests` tự bật lại.
- Không commit `data/`, `.env`, `*.log`. Không có bí mật trong repo.
