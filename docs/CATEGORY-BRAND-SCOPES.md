# Danh mục – thương hiệu và phạm vi ưu đãi (30/09/2026)

## Hành vi

- Danh mục có `brandIds`, lưu many-to-many qua `category_brands`. Form thêm/sửa có **Thương hiệu áp dụng**, tìm kiếm, checkbox, chip bỏ từng lựa chọn và tải thêm từng trang.
- Mega menu và bộ lọc của danh mục lấy đúng các hãng được gán, bao gồm hãng chưa có sản phẩm. Khi chưa chọn danh mục, bộ lọc hãng lấy toàn bộ hãng trong DB.
- Đổi danh mục trong form sản phẩm sẽ xóa lựa chọn hãng cũ và tải hãng được phép. Backend cũng kiểm tra cặp category/brand và khóa danh mục khi ghi để tránh lưu đồng thời tạo quan hệ không hợp lệ.
- Không cho bỏ một hãng khỏi danh mục nếu sản phẩm hiện có còn dùng cặp đó (kể cả sản phẩm ngừng bán). Chuyển sản phẩm sang hãng/danh mục phù hợp trước; không tự sửa hoặc xóa sản phẩm.
- Promotion/Voucher đều dùng `scopeType` và chỉ hiện một bộ chọn tương ứng. Đổi scope sẽ xóa lựa chọn cũ; backend cũng chuẩn hóa, không tin vào việc ẩn field ở frontend.
- Bỏ nhóm thông số Phiên bản/Kết nối khỏi bộ lọc; không xóa thông số ở trang sản phẩm. Bảo hành dùng `Product.warrantyMonths`, khoảng min/max động, thanh kéo hai đầu theo tháng, tự áp dụng khi thả. Query giữ lại khi tải trang/chuyển hãng. Nếu mọi sản phẩm có cùng thời hạn thì hiển thị số tháng cố định.
- Không thay quy tắc giỏ hàng, kiểm tra quyền, vòng đời đơn, banner tự chuyển hoặc layout admin của đợt trước.

## Quy tắc tính giảm

| scopeType | Danh sách lưu | Sản phẩm hợp lệ |
| --- | --- | --- |
| ALL | Cả ba danh sách rỗng | Toàn bộ sản phẩm |
| CATEGORY | categoryIds | Có category thuộc bất kỳ ID được chọn |
| BRAND | brandIds | Có brand thuộc bất kỳ ID được chọn |
| PRODUCT | productIds | ID sản phẩm thuộc bất kỳ ID được chọn |

CATEGORY/BRAND/PRODUCT phải có ít nhất một ID, các ID phải tồn tại. Danh sách không thuộc scope bị xóa khi lưu; ALL không giữ quan hệ dư. Nhiều mục trong cùng scope dùng OR.

Giá sản phẩm/phiên bản lấy mức khuyến mãi đang hiệu lực cao nhất trong các scope phù hợp, không cộng dồn phần trăm. Catalog, chi tiết và checkout dùng cùng giá do backend tính. Voucher chỉ giảm trên tổng tiền của dòng hàng hợp lệ **sau khuyến mãi**; minimum order vẫn tính trên subtotal của các dòng được checkout như nghiệp vụ cũ. Active, thời hạn, giảm tối đa, usage limit và khóa voucher khi đặt hàng được giữ nguyên.

## Migration

`src/main/resources/db/migration/V6__category_brands_and_offer_scopes.sql`:

1. Tạo `category_brands`, khóa chính ghép và foreign key; backfill tất cả cặp khác null từ products, bao gồm sản phẩm ngừng bán.
2. Thêm `scope_type` vào promotions/vouchers; tạo `promotion_category`, `voucher_brand`, `voucher_product`. Giữ bảng quan hệ cũ dùng lại cho các scope còn lại.
3. Voucher cũ `is_all` chuyển sang ALL/CATEGORY; xóa liên kết category thừa của voucher ALL.
4. Promotion chỉ gán hãng chuyển BRAND, chỉ gán sản phẩm giữ PRODUCT. Promotion cũ có cả hãng và sản phẩm được tách thành hai chương trình tương đương: chương trình gốc BRAND và bản sao PRODUCT cùng mức giảm/thời hạn. Cách này giữ OR cũ, kể cả sản phẩm mới thêm vào hãng; giá vẫn lấy mức cao nhất.

Không sửa V1–V5, không đổi `ddl-auto=validate`. Flyway chạy V6 một lần khi khởi động. MySQL `techshop` hiện ở V6; bản chụp dữ liệu sau V6 nằm trong `data/backups/techshop-scope-snapshot-20260930-085507.sql`. Kiểm thử nâng cấp dữ liệu legacy thực hiện trên schema MySQL riêng, không reset DB chính.

## API

- `GET /api/v1/admin/options/{resource}`: resource = brands/categories/products; `search`, `page` (20 mục/trang), `ids` để nạp nhãn các lựa chọn đã lưu. `categoryId` giới hạn hãng theo category. Chỉ ADMIN.
- POST/PUT/GET category hiện có thêm `brandIds`; product giữ categoryId/brandId và thêm validation quan hệ.
- POST/PUT/GET promotion/voucher hiện có thêm `scopeType`, `categoryIds`, `brandIds`, `productIds`.
- `GET /api/v1/catalog/menu` và `/catalog/facets`: hãng theo cấu hình category; facets thêm `minWarranty`, `maxWarranty`.
- `GET /api/v1/products`: thêm `minWarranty`, `maxWarranty`; `promotionId` hiểu đủ bốn scope trước phân trang.
- `GET /api/v1/offers`: thêm `scopeType`, `scopeNames` để trình bày điều kiện voucher và kiểm tra sản phẩm áp dụng.
- Payload voucher cũ `all/categoryIds`, promotion cũ chỉ brand hoặc chỉ product vẫn nhận. Payload promotion cũ có cả hai phải chọn scope rõ ràng; dữ liệu cũ trong DB được migration xử lý như trên.

## File thay đổi trong đợt này

| Nhóm | File |
| --- | --- |
| Entity/domain | `src/main/java/com/example/shop/domain/{Category,Product,Promotion,Voucher,ScopeType,ScopedOffer}.java` (hai file Scope mới) |
| Repository | `src/main/java/com/example/shop/repository/CategoryRepository.java`, `OfferScopeSpecification.java` (mới) |
| Service | `src/main/java/com/example/shop/api/service/AdminCatalogService.java`, `CommerceService.java` |
| Controller | `src/main/java/com/example/shop/api/controller/{AdminApiController,CatalogApiController,OffersApiController}.java` |
| Seed tương thích | `src/main/java/com/example/shop/config/TechDemoData.java` |
| Migration mới | `src/main/resources/db/migration/V6__category_brands_and_offer_scopes.sql` |
| Giao diện | `frontend/src/components/SearchMultiSelect.vue` (mới), `RangeSlider.vue`, `HomeHero.vue`, `frontend/src/views/{AdminPage,CatalogPage}.vue`, `frontend/src/lib/admin.js`, `frontend/src/{admin-layout,catalog}.css` |
| Test Java | `src/test/java/com/example/shop/ScopeApiTests.java` (mới), `AdminApiTests.java`, `LegacyMigrationTest.java` |
| Test browser | `frontend/tests/scopes.spec.js` (mới), `catalog.spec.js`, `experience.spec.js`, `selling.spec.js`, `shop.spec.js` |
| Tài liệu | `docs/CATEGORY-BRAND-SCOPES.md` (mới), `API.md`, `MIGRATION.md`, `TECHSHOP-COMPLETION.md` |

Vue là giao diện đang hoạt động; không sửa template JSP đã lưu trong legacy. Những file thay đổi từ các đợt trước trong working tree được giữ nguyên.

## Test nhanh

1. Admin → Danh mục → Sửa: chọn hai hãng, lưu rồi mở lại; chip phải còn đủ. Bỏ một hãng chưa được dùng và lưu; hãng đó biến mất khỏi menu/bộ lọc danh mục. Thử bỏ hãng đang có sản phẩm: nhận thông báo, dữ liệu không bị đổi.
2. Admin → Sản phẩm → Thêm mới: chọn danh mục A; danh sách hãng chỉ còn các hãng của A. Chọn một hãng, đổi sang B: lựa chọn cũ bị xóa. Gửi API với cặp không được gán: HTTP 400.
3. Mỗi module Khuyến mãi/Voucher: chọn ALL rồi lưu; mở lại không có selector phụ. Chọn CATEGORY và hai danh mục; tìm/đánh dấu, lưu/mở lại đủ chip. Lặp với BRAND và PRODUCT. Bỏ hết lựa chọn của scope khác ALL rồi lưu: backend báo lỗi. Chuyển lại ALL: GET chi tiết trả ba danh sách rỗng.
4. Với các sản phẩm A thuộc C1/B1, B thuộc C2/B2, C thuộc C3/B1: giảm 10% ALL áp dụng cả ba; CATEGORY C1+C2 áp dụng A+B; BRAND B1 áp dụng A+C; PRODUCT A+B chỉ đúng A+B. Kiểm tra cả giá khuyến mãi và voucher trong báo giá giỏ hàng.
5. Danh mục có sản phẩm bảo hành 12 và 36 tháng: thanh kéo hiện 12–36. Kéo đầu trái đến 24 rồi thả, chỉ còn sản phẩm 36 tháng; reload vẫn giữ query. Không còn nhóm Phiên bản/Kết nối; thông số khác như RAM vẫn lọc được.

## Kiểm chứng tự động

- `./mvnw.cmd test`: 44 test đạt, 1 migration test bỏ qua theo cấu hình; log `data/scope-tests.log`.
- Kiểm thử legacy migration MySQL riêng: đạt V1 → V6, kiểm tra dữ liệu cũ/backfill/tách khuyến mãi và migrate lần hai không thay đổi; log `data/scope-migration-test.log`.
- Frontend build và JAR package đạt: `data/scope-build-final.log`, `data/scope-package-final.log`.
- Lượt browser đầy đủ: 13 ca cũ đạt, phát hiện lỗi padding chung làm lệch nút kéo bảo hành. Đã sửa CSS và chạy lại ca scope/drag đạt (1/1). Log `data/scope-e2e-final.log`, `data/scope-e2e-retest.log`. Các lượt E2E tiếp theo dùng DB H2 riêng theo P0 của [ARCHITECTURE-IMPLEMENTATION.md](ARCHITECTURE-IMPLEMENTATION.md).
