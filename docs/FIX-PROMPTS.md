# Việc còn lại và prompt để sửa

Cập nhật 2026-09-30, sau khi làm lại phạm vi ưu đãi, form khuyến mãi/voucher và nhãn giảm giá. Nhánh `appmod/java-upgrade-20260917082931`, chưa commit.

## 1. Kết quả quét toàn bộ test

| Hạng mục | Kết quả | Ghi chú |
| --- | --- | --- |
| Backend `.\mvnw.cmd test` (H2) | 99 chạy, 0 lỗi, 1 bỏ qua | Bỏ qua là `LegacyMigrationTest` (cần MySQL cô lập) |
| `LegacyMigrationTest` trên MySQL 8.0.44 cô lập (cổng 3308, thư mục tạm) | Đạt | Chạy V1–V11, có dữ liệu `CATALOG` cũ đủ ba dạng |
| `OfferScopeTests`, `ScopeApiTests` trên MySQL, Flyway bật, `ddl-auto=validate` | 21 đạt | Schema và entity khớp |
| `npm run build` | Đạt | |
| E2E Playwright (`frontend/tests`, 14 ca) | 14/14 | `catalog.spec.js` dễ vỡ, xem mục 2.1 |
| Quét giao diện bằng ảnh chụp (desktop và 390 px) | Không lỗi console | Các điểm chưa ổn ở mục 2 |

Đã làm trong đợt này: bốn chế độ phạm vi (`ALL`, `CATEGORY`, `BRAND`, `PRODUCT`) không kết hợp, mỗi nhóm chọn cả nhóm hoặc từng sản phẩm; migration V11; API danh sách chọn có ảnh và số sản phẩm; form khuyến mãi/voucher mới (`OfferForm.vue`, `ScopePicker.vue`, `ScopeGroupBrowser.vue`, `ScopeProductList.vue`); nhãn "Giảm X%" và "Tiết kiệm" trên thẻ sản phẩm, trang chi tiết, giỏ hàng và bản xem trước.

## 2. Việc chưa ổn

### 2.1 `catalog.spec.js` dễ vỡ, menu danh mục trên mobile

Không do mã của đợt này: giao diện của đợt này chạy với backend `HEAD` thì đạt, và ảnh chụp hai lần chỉ khác nhau 3 px do chữ số trong tên danh mục thử. Trên khung 390 px, nút mũi tên của mục danh mục cuối bị panel `.mega-detail` đè lên nếu tên danh mục dài thêm vài pixel. Ca này đặt tên danh mục theo `Date.now()` nên lúc đạt lúc hỏng. Lỗi in ra là `Test timeout` ở bước DELETE dọn dẹp, nhưng nguyên nhân thật là lệnh click `Xem nhóm facet-…` bị `.mega-detail intercepts pointer events`.

```
Bối cảnh: TechShop tại D:\food_shop. Ca frontend/tests/catalog.spec.js đạt hay hỏng tùy độ dài tên danh mục thử (facet-<Date.now()>). Ở khung 390x844, nút mũi tên (aria-label "Xem nhóm ...") của mục cuối trong .mega-category-list bị .mega-detail đè lên (Playwright báo "<div class=mega-detail> intercepts pointer events"). Xem frontend/src/components/CategoryMenu.vue và frontend/src/catalog.css.
Việc: (1) sửa CSS để cột danh sách không tràn: min-width:0 cho từng dòng, tên danh mục ngắt dòng bằng overflow-wrap:anywhere, nút mũi tên flex-shrink:0 và nằm trọn trong cột danh sách ở mọi độ rộng từ 320 px. (2) Thêm một ca kiểm tra kích thước: tên danh mục thử dài 40 ký tự, khung 320 px và 390 px, nút mũi tên click được. (3) Không đổi logic chọn danh mục bằng hover/focus.
Xong khi: chạy catalog.spec.js 5 lần liên tiếp đều đạt; npm.cmd --prefix frontend run build đạt.
```

### 2.2 Chưa commit và cách triển khai lên máy của bạn

Cây làm việc gồm phần của đợt này và phần P8 dở của agent trước (tách `AdminCatalogService` thành các service/controller riêng). Đã kiểm thử chung, chưa commit.

- Backend dev ở cổng 8080 đang chạy bản cũ (`/api/v1/offers` chưa có `scopeDescription`). Cần dừng và chạy lại để có V11 và giao diện mới.
- V11 sửa dữ liệu trong `promotions`, `vouchers` và các bảng nối. Sao lưu schema `techshop` trước khi chạy (`mysqldump`).
- Ưu đãi cũ chọn cả danh mục lẫn hãng (kiểu `CATALOG` giao nhau) được đổi thành danh sách sản phẩm cố định, nên sản phẩm thêm sau vào hãng/danh mục đó sẽ không tự được giảm. Ưu đãi cũ chỉ một nhóm giữ nguyên nhóm.
- Nhãn giảm giá chỉ hiện khi có khuyến mãi đang chạy phủ sản phẩm. Khuyến mãi mẫu `TECH-DEMO-ORBIT` chỉ phủ hãng Orbit Demo nên các sản phẩm khác (Chuột, Điện thoại…) không có nhãn cho đến khi tạo khuyến mãi phủ chúng. Form mới có bản xem trước hiện chính xác sản phẩm nào sẽ có nhãn.

```
Bối cảnh: TechShop tại D:\food_shop, nhánh appmod/java-upgrade-20260917082931, cây làm việc chưa commit. Chia thành các commit theo nhóm, không dùng --no-verify, giữ kiểu xuống dòng từng file:
(1) refactor(admin): tách AdminCatalogService thành service/controller theo tài nguyên (các file api/controller/*AdminController.java, api/service/*AdminService.java, AdminSupport, AdminInputReader, JsonInputConfig);
(2) feat(offers): phạm vi ALL/CATEGORY/BRAND/PRODUCT theo hợp, migration V11, OfferScopeText, API options có ảnh/số sản phẩm, nhãn giảm giá trong dữ liệu sản phẩm và giỏ hàng, kèm test Java;
(3) feat(admin-ui): form khuyến mãi/voucher, bộ chọn phạm vi, nhãn giảm giá ở storefront, cập nhật E2E và tài liệu.
Trước mỗi commit chạy .\mvnw.cmd test và npm.cmd --prefix frontend run build.
```

### 2.3 P8 chưa xong

Đã có bước 1–2 của Prompt B trong `docs/CODEX-HANDOFF.md` (DTO có validation, tách service). Chưa làm: bước 3 (enum `OrderStatus`/`PaymentStatus`), bước 4 (xóa code chết, bộ scaffold TypeScript `main.ts`, `App.vue`, `router/index.ts`, `stores/*.ts`… vẫn còn), bước 5 (`spring.jpa.open-in-view=true` vẫn bật ở `application.properties` và `application-test.properties`). Dùng lại Prompt B ở `docs/CODEX-HANDOFF.md`, bỏ hai bước đã xong.

### 2.4 Các form admin khác còn kiểu cũ

Sản phẩm, danh mục, thương hiệu, banner, chính sách, người dùng vẫn dùng form dọc chung trong `AdminPage.vue` (mỗi trường một hàng, không nhóm, không xem trước). Ô chọn thương hiệu trong form danh mục dùng `SearchMultiSelect.vue`, không có logo. Đã sửa riêng lỗi xếp dọc ô tick/tên trong danh sách của ô này.

```
Bối cảnh: TechShop tại D:\food_shop. Form khuyến mãi/voucher đã làm lại bằng frontend/src/components/OfferForm.vue (các khối Thông tin, Mức giảm, Thời gian, Phạm vi, có xem trước). Hãy áp dụng cùng phong cách cho form Danh mục, Thương hiệu, Banner và Sản phẩm trong frontend/src/views/AdminPage.vue: tách mỗi tài nguyên thành một component form (frontend/src/components/admin/*Form.vue), chia khối có tiêu đề, có xem trước ảnh/logo ngay trong khối, ô chọn thương hiệu của form Danh mục dùng thẻ có logo (tái dùng ScopeProductList/ScopeGroupBrowser nếu hợp lý), nhãn <label for> tách khỏi phần gợi ý để getByLabel khớp chính xác. Giữ nguyên các nhãn mà frontend/tests dùng (grep tests trước khi đổi nhãn). Kiểm tra ở 390 px và 1400 px, không tràn ngang.
Xong khi: npm.cmd --prefix frontend run build đạt; E2E đạt 14/14; chụp ảnh từng form desktop và mobile đính kèm.
```

### 2.5 Khoảng trống kiểm thử

- Frontend chưa có unit test (không có vitest); các luồng bộ chọn phạm vi chỉ được E2E `scopes.spec.js` phủ.
- Chưa có E2E cho nhãn giảm giá trong giỏ hàng và dải khuyến mãi ở trang chi tiết, cho bộ chọn phạm vi ở 390 px, cho lọc voucher theo danh mục.
- Bộ lọc danh mục ở danh sách voucher (`AdminSupport.matching`) chỉ lấy voucher toàn cửa hàng hoặc chọn cả danh mục đó, chưa lấy voucher chọn lẻ sản phẩm trong danh mục.

```
Bối cảnh: TechShop tại D:\food_shop. Bổ sung kiểm thử, không đổi hành vi:
(1) thêm vitest cho frontend với test cho frontend/src/lib/scopeMeta.js và cho logic gộp thay đổi của ScopeGroupBrowser.vue/ScopePicker.vue (chọn cả nhóm xóa các sản phẩm lẻ của nhóm đó; đổi chế độ xóa lựa chọn; bỏ chip);
(2) thêm vào frontend/tests/scopes.spec.js: giỏ hàng hiện "Giảm X%" và giá gốc gạch ngang khi sản phẩm có khuyến mãi; trang chi tiết hiện dải khuyến mãi có tên và ngày kết thúc; bộ chọn phạm vi ở khung 390 px không tràn ngang (scrollWidth <= innerWidth);
(3) sửa AdminSupport.matching để bộ lọc danh mục của voucher lấy thêm voucher có sản phẩm lẻ thuộc danh mục đó, kèm test Java trong OfferScopeTests.
Xong khi: .\mvnw.cmd test và npm.cmd --prefix frontend run build đạt; E2E 14/14 trở lên.
```

## 3. Ghi chú môi trường

- Chạy E2E khi `frontend/test-results` và `playwright-report` bị khóa: `npx playwright test --reporter=list --output=../data/pw-output`. `frontend/dist` cũ bị khóa đã đổi tên thành `frontend/dist-stale-build2` (loại khỏi git bằng `.git/info/exclude`); có thể xóa khi có quyền.
- Kiểm thử migration: `mysqld --no-defaults --initialize-insecure --datadir=<thư mục tạm>`, chạy cổng 3308, tạo database `techshop_migration_<tên>`, đặt `MIGRATION_TEST_URL=jdbc:mysql://127.0.0.1:3308/<db>?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&allowMultiQueries=true`, `MIGRATION_TEST_USERNAME=root`, rồi `.\mvnw.cmd -Dtest=LegacyMigrationTest test`. Tắt MySQL sau khi xong.
- Khi dọn một git worktree có liên kết `node_modules`, gỡ liên kết trước (`cmd /c rmdir <đường dẫn>`), nếu không `git worktree remove` sẽ xóa qua liên kết vào `node_modules` thật.
