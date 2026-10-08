# Prompt cho Codex — BACKEND (Spring Boot)

> Dán toàn bộ nội dung dưới đây vào Codex. Đây là **phần backend**. Một Codex khác làm frontend — tuyệt đối không đụng vào `frontend/`.

---

Bạn là kỹ sư backend của dự án TechShop (Spring Boot 3 + JPA + MySQL/H2, Maven, thư mục gốc `d:\food_shop`).

## Mục tiêu

Storefront sắp bổ sung 2 thứ cần dữ liệu từ backend:

1. Thẻ **"Khuyến mãi đang diễn ra"** và thẻ **"Mã giảm giá"** ở trang chủ phải hiển thị được **ảnh banner do admin upload**.
2. Có một **trang voucher riêng** gồm nhiều banner giảm giá → cần banner được phân loại theo **vị trí hiển thị**.

Việc của bạn: thêm đúng các trường dữ liệu + API đó. **Không làm giao diện.**

## Hợp đồng API phải tạo ra (frontend đã được viết dựa trên đúng hợp đồng này — không được đổi tên trường)

### 1. Ảnh cho Promotion và Voucher

- Thêm cột `image_url VARCHAR(500) NULL` vào bảng `promotions` và `vouchers`.
- `Promotion.imageUrl`, `Voucher.imageUrl` (String, nullable) + getter/setter.
- `GET /api/v1/offers` (public): mỗi phần tử trong `promotions` và trong `vouchers` thêm khoá `imageUrl`. Null → trả chuỗi rỗng `""`, **không** trả `null`, không bỏ khoá.
- `GET /api/v1/home` (public): object `promotion` thêm khoá `imageUrl` (cùng quy ước chuỗi rỗng).
- Admin:
  - `PromotionInput` và `VoucherInput` thêm `@Size(max = 500) String imageUrl` (không bắt buộc nhập).
  - `PromotionAdminService` / `VoucherAdminService`: `save` lưu giá trị này (dùng đúng helper chuẩn hoá URL mà `BannerAdminService.save` đang dùng cho `imageUrl`), `list` và `detail` trả thêm `imageUrl`.

Ảnh được upload bằng endpoint **đã có sẵn** `POST /api/v1/admin/uploads` (trả `{"url":"/uploads/<uuid>.<ext>"}`). **Không viết endpoint upload mới, không sửa `UploadApiController`.**

### 2. Vị trí hiển thị cho Banner

- Enum mới `com.example.shop.domain.BannerPlacement` với đúng 2 giá trị: `HOME_HERO`, `VOUCHER_PAGE`. **Không thêm giá trị nào khác.**
- `Banner.placement`: `@Enumerated(EnumType.STRING)`, cột `placement VARCHAR(20) NOT NULL DEFAULT 'HOME_HERO'`. Mọi banner đang có trong DB phải được migrate thành `HOME_HERO` để trang chủ hiện tại không đổi hành vi.
- `GET /api/v1/banners` (public, đang có): nhận thêm query param **tuỳ chọn** `placement`.
  - Không truyền → trả banner `HOME_HERO` (giữ nguyên đúng hành vi cũ: chỉ banner `active` + `technologyCatalog`, sắp xếp id giảm dần).
  - `?placement=VOUCHER_PAGE` → trả banner của vị trí đó, cùng bộ lọc và thứ tự.
  - Giá trị không hợp lệ → HTTP 400 theo đúng cơ chế lỗi sẵn có của dự án, không ném 500.
  - Payload mỗi banner giữ nguyên các khoá cũ (`id`, `name`, `imageUrl`, `link`) và thêm `placement`.
- Admin: `BannerInput` thêm `BannerPlacement placement` (null → `HOME_HERO`); `BannerAdminService` `list`/`detail`/`save` xử lý trường này.

### 3. Không cần endpoint nào khác

Trang voucher của frontend sẽ ghép dữ liệu từ `GET /api/v1/offers` + `GET /api/v1/banners?placement=VOUCHER_PAGE`. **Không tạo controller/endpoint mới.**

## Ràng buộc kỹ thuật bắt buộc

- `spring.jpa.hibernate.ddl-auto=validate` → **bắt buộc** có migration Flyway mới `src/main/resources/db/migration/V12__offer_images_and_banner_placement.sql`. Migration cũ (V1–V11) **không được sửa, không được đổi tên**.
- Migration phải chạy được trên cả MySQL và H2 (xem các file V1–V11 để theo đúng phong cách SQL đang dùng).
- Chỉ **thêm** trường vào payload. Không đổi tên, không xoá, không đổi kiểu bất kỳ khoá nào đang có — frontend hiện tại đang đọc chúng.
- Giữ nguyên quy tắc phân quyền hiện có: `/api/v1/banners` và `/api/v1/offers` là public; mọi thứ dưới `/api/v1/admin/**` yêu cầu quyền admin. Không sửa `ApiSecurityConfig` trừ khi thực sự cần cho `placement` (nếu cần thì giải thích lý do trong báo cáo).
- Giữ nguyên code style của từng file bạn sửa (dự án viết khá dồn dòng — **không reformat**, không sắp xếp lại import, không "dọn dẹp" code không liên quan).
- `HomeReadService` có cache `shop.home-cache-seconds` — chỉ thêm khoá vào payload, không động vào cơ chế cache.

## Phạm vi file — chỉ được sửa/tạo trong danh sách này

Được phép:

- `src/main/java/com/example/shop/domain/Promotion.java`, `Voucher.java`, `Banner.java`
- `src/main/java/com/example/shop/domain/BannerPlacement.java` (tạo mới)
- `src/main/java/com/example/shop/api/dto/admin/PromotionInput.java`, `VoucherInput.java`, `BannerInput.java`
- `src/main/java/com/example/shop/api/service/PromotionAdminService.java`, `VoucherAdminService.java`, `BannerAdminService.java`, `OfferReadService.java`, `HomeReadService.java`, `CatalogReadService.java`
- `src/main/java/com/example/shop/api/controller/CatalogApiController.java` (chỉ để nhận query param `placement`)
- `src/main/resources/db/migration/V12__offer_images_and_banner_placement.sql` (tạo mới)
- `src/test/java/...` — thêm test mới hoặc bổ sung assert cho trường mới
- `docs/API.md` — ghi 3 gạch đầu dòng về `imageUrl` và `placement`

**Nghiêm cấm:**

- Sửa bất cứ file nào trong `frontend/` — kể cả CSS, kể cả một dòng.
- Sửa `UploadApiController`, `UploadWebConfig`, cơ chế thanh toán, giỏ hàng, đơn hàng, auth, chat.
- Sửa hoặc xoá migration V1–V11.
- Xoá hoặc sửa test đang có để cho nó pass. Nếu một test cũ fail vì thay đổi của bạn, đó là tín hiệu bạn đã phá hợp đồng — sửa code của bạn, không sửa test.
- Đổi version dependency trong `pom.xml`, đổi cấu hình trong `application*.properties`.
- "Tiện tay" refactor, đổi tên biến, thêm Lombok, đổi kiểu `Date` sang `Instant`, đổi code không nằm trong yêu cầu.

## Kiểm chứng trước khi báo xong

Chạy và dán kết quả thật:

```
.\mvnw.cmd -q test
```

Toàn bộ test phải xanh, đặc biệt `HomeApiTests`, `ExperienceApiTests`, `AdminApiTests`, `OfferScopeTests`, `LegacyMigrationTest`.

## Báo cáo cuối

Liệt kê: file đã sửa/tạo, nội dung migration V12, payload JSON mẫu của `GET /api/v1/offers` và `GET /api/v1/banners?placement=VOUCHER_PAGE` sau thay đổi, kết quả chạy test. Nếu phát hiện việc cần làm nhưng nằm ngoài phạm vi trên → **ghi ra, không tự ý làm**. Không tự commit, không tự push.
