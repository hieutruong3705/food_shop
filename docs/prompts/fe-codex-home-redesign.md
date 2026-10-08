# Prompt cho Codex — FRONTEND (Vue 3 + Vite)

> Dán toàn bộ nội dung dưới đây vào Codex. Đây là **phần frontend**. Một Codex khác làm backend — tuyệt đối không đụng vào `src/main/java/`.

---

Bạn là kỹ sư frontend của dự án TechShop (Vue 3 `<script setup>` + Vite + vue-router + Pinia, thư mục `d:\food_shop\frontend`).

Giao diện vừa được đổi sang tông **xanh dương** (`--brand: #0075ab`, `--accent: #daebf3`, định nghĩa ở đầu `src/retail.css`). **Giữ nguyên hệ màu này** — không được đổi token màu, không tự chọn tông khác.

## Dữ liệu backend bạn được dùng

Backend (do người khác làm, theo đúng hợp đồng này) cung cấp:

- `GET /api/v1/offers` → `{ promotions: [...], vouchers: [...] }`. Mỗi promotion có `id, name, description, discountRate, endDate, hasAvailableProducts, scopeDescription, imageUrl`. Mỗi voucher có `code, discount, minimumOrder, maximumDiscount, endDate, scopeDescription, imageUrl`.
- `GET /api/v1/home` → `promotion` (có thêm `imageUrl`), `brands`, `bestSellers`, `categoryRows`, `reviews`.
- `GET /api/v1/banners` → banner trang chủ (như hiện tại).
- `GET /api/v1/banners?placement=VOUCHER_PAGE` → banner cho trang voucher.
- Mỗi banner: `{ id, name, imageUrl, link, placement }`.

**Quan trọng:** `imageUrl` có thể là chuỗi rỗng. Mọi chỗ dùng ảnh phải có nhánh "không có ảnh" hiển thị đẹp, không để khung vỡ hay khoảng trắng mồ côi. Dùng `imageUrl()` và `imageError()` từ `src/lib/shop.js` như các chỗ khác trong dự án.

Nếu backend chưa deploy kịp, code vẫn phải chạy được (field thiếu → coi như rỗng), **không được crash trang chủ**.

## 7 việc cần làm

### 1. Làm lại footer (file `src/RestApp.vue`, CSS ở `src/home.css`)

Thay footer hiện tại bằng bố cục nhiều tầng:

1. **Dải đăng ký nhận tin**: nền `var(--brand)`, chữ trắng, chạy hết chiều ngang. Bên trái tiêu đề "Đăng ký nhận tin" + dòng phụ ưu đãi; bên phải một form gồm input email (bo tròn) + nút "Đăng ký". Dự án **không có API newsletter** → form chỉ validate định dạng email phía client rồi hiện thông báo cảm ơn qua `role="status"`. **Không gọi API, không bịa endpoint.**
2. **Hàng link ngang** trên nền trắng, có đường kẻ dưới: chỉ trỏ tới route có thật (`/products`, `/policies`, `/orders`, `/account`, `/cart`). **Không tạo link chết, không tạo route mới ở bước này.**
3. **4 cột**:
   - "Liên hệ TechShop": địa chỉ, điện thoại, email (dùng dữ liệu đang có trong `src/views/PolicyPage.vue` nếu có, nếu không thì giữ nguyên các chuỗi đang dùng ở footer cũ), kèm hàng icon mạng xã hội dùng `ShopIcon` — chỉ dùng icon đã có trong `src/components/ShopIcon.vue`, **không thêm thư viện icon mới**.
   - "Danh mục": 6 danh mục đầu từ `categories` (đang có sẵn trong `RestApp.vue`).
   - "Dịch vụ khách hàng" và "Thông tin": các link route có thật.
4. **Đáy**: dòng bản quyền bên trái, bên phải là hàng badge phương thức thanh toán. Dự án chỉ hỗ trợ **COD và VNPay** → chỉ hiện hai badge đó dưới dạng chữ/khối màu. **Không nhúng logo Visa/Mastercard/PayPal** vì dự án không có file logo đó và không được phép bịa.

Yêu cầu: responsive (4 cột → 2 cột ở ≤1000px → 1 cột ở ≤650px), tương phản chữ/nền tối thiểu 4.5:1, dùng token màu sẵn có.

### 2. Hai thẻ ưu đãi ở hero cho hiển thị ảnh (`src/components/HomeHero.vue`, `src/components/OfferCarousel.vue`, CSS ở `src/experience.css`)

- Thẻ **"Khuyến mãi đang diễn ra"**: nếu `promotion.imageUrl` có giá trị thì hiện ảnh ở đầu thẻ (tỉ lệ 16/9, `object-fit: cover`, bo góc trên). Cả vùng ảnh phải bấm được, dẫn tới `{ path: '/products', query: { promotionId: promotion.id } }` — đúng link mà dòng "Xem sản phẩm áp dụng" đang dùng. Khi `hasAvailableProducts === false` thì ảnh **không** là link, giữ nguyên thông báo "Sản phẩm áp dụng hiện chưa được mở bán".
- Thẻ **"Mã giảm giá"**: nếu `voucher.imageUrl` có giá trị thì hiện ảnh tương tự. Bổ sung một link cố định dưới thẻ: **"Xem tất cả mã giảm giá →"** dẫn tới `/vouchers` (trang tạo ở việc số 3). Giữ nguyên nút sao chép mã và thông báo `role="status"`.
- Giữ nguyên cơ chế carousel tự chạy, nút ‹ ›, `prefers-reduced-motion`, và toàn bộ thuộc tính a11y (`aria-label`, `aria-roledescription`) đang có.

### 3. Trang voucher mới `/vouchers`

- Tạo `src/views/VoucherPage.vue`, đăng ký route `/vouchers` trong `src/router/index.js` theo đúng kiểu lazy import như các route khác. Route **public**, không `meta.auth`.
- Nội dung:
  - Tiêu đề trang + mô tả ngắn.
  - **Lưới banner giảm giá**: gọi `GET /banners?placement=VOUCHER_PAGE`, mỗi banner là ảnh bấm được dẫn tới `banner.link`. 2 cột ở desktop, 1 cột ở mobile. Không có banner → không render khối này (không hiện khung rỗng).
  - **Danh sách mã giảm giá**: gọi `GET /offers`, render tất cả `vouchers` dạng thẻ: mức giảm, mã, nút sao chép, đơn tối thiểu, giảm tối đa, hạn dùng, `scopeDescription`.
- Dùng `useTask` từ `src/lib/shop.js` + `TaskState` cho trạng thái tải/lỗi, và có trạng thái rỗng tử tế khi chưa có mã nào.

### 4. Thiết kế lại khối "Ưu đãi đang diễn ra" (`src/components/home/PromoRow.vue`, CSS ở `src/home.css`)

Bố cục mới, từ trên xuống:

1. **Banner ảnh** `promotion.imageUrl` trải hết chiều ngang khối, bo góc trên, `object-fit: cover`, chiều cao cố định (khoảng 150–200px desktop, thấp hơn ở mobile). Không có ảnh → bỏ hẳn phần banner và giữ phần đầu dạng chữ như hiện tại (nền gradient hiện có).
2. **Dải thông tin**: tên chương trình + badge "Giảm X%" bên trái; **đồng hồ đếm ngược** bên phải dạng các ô số nhỏ liền nhau có nhãn ngày/giờ/phút/giây.
3. **Lưới sản phẩm** dùng `ProductCard` như hiện tại.
4. Link "Xem tất cả sản phẩm ưu đãi →" giữ nguyên.

**Không thêm tab lọc danh mục vào khối này.** Giữ nguyên logic đếm ngược đang có (`setInterval` + dọn trong `onUnmounted`) và điều kiện `v-if="left > 0"`.

### 5. Bỏ thanh cuộn ở sidebar danh mục

Trong `src/experience.css`, rule `.catalog-sidebar{max-height:470px;overflow:auto}` đang tạo thanh cuộn trong sidebar "Danh mục sản phẩm" ở trang chủ. Bỏ `max-height` và `overflow` để sidebar cao tự nhiên theo nội dung. Kiểm tra lại lưới `.retail-hero-grid` không bị vỡ ở mọi breakpoint.

### 6. Bỏ hiệu ứng phủ màu trên banner trang chủ

Trong `src/experience.css`, `.banner-caption` đang phủ `linear-gradient(transparent,#005b85de)` lên ảnh banner làm ảnh bị ám xanh. Yêu cầu: **ảnh banner hiển thị nguyên bản**.

- Bỏ lớp gradient phủ.
- Banner do admin upload thường đã có sẵn chữ trên ảnh → khi `banners` từ API có dữ liệu thì **ẩn hẳn `.banner-caption`**, chỉ giữ caption cho 3 slide mặc định (`/tech/*.svg`) để trang không trống khi chưa có banner.
- Giữ nguyên nút ‹ ›, chấm tròn chỉ số, bộ đếm "1 / N" và hiệu ứng chuyển slide.

### 7. Thiết kế lại khối "Gợi ý dành cho bạn" (`src/views/TechHome.vue`, CSS ở `src/retail.css`)

- **Bỏ hoàn toàn hàng tab lọc** "Tất cả / Điện thoại / Máy tính bảng / …" (`.retail-filter-tabs`). Xoá luôn state không còn dùng trong `TechHome.vue`: `selectedCategory`, `filters`, `shown` → render thẳng `products`.
- **Thêm một dải banner tự thiết kế** ở đầu khối — **không lấy từ API, không có upload, không thêm ảnh mới vào repo**. Dải banner này bạn tự dựng bằng HTML/CSS:
  - Nền dùng gradient từ token màu sẵn có (`var(--brand)` → một sắc xanh nhạt hơn đã có trong file CSS), bo góc trên, cao khoảng 120–150px ở desktop và thấp hơn ở mobile.
  - Bên trái: dòng nhãn nhỏ viết hoa + một tiêu đề ngắn (ví dụ "Gợi ý dành cho bạn" / "Chọn đúng thiết bị bạn cần") + nút/link "Xem tất cả →" trỏ tới `/products?sort=popular`.
  - Bên phải: trang trí bằng **ảnh SVG đã có sẵn** trong `frontend/public/tech/` (`laptop.svg`, `headphones.svg`, `phone.svg`, `monitor.svg`… — chọn 2–3 cái), đặt nghiêng/chồng nhẹ, `aria-hidden="true"` vì chỉ là trang trí. Ẩn phần trang trí này ở ≤650px để không chen chỗ chữ.
  - Chữ trên dải banner phải đạt tương phản ≥4.5:1 với nền.
- Đổi nền khối từ xanh đậm sang **nền trắng, viền nhạt, bo góc** để sản phẩm nổi lên (giống bố cục tham chiếu); tiêu đề và link "Xem tất cả" đổi sang màu chữ tối/`var(--brand)` cho đủ tương phản trên nền trắng. Kiểm tra `.featured-retail .product-image`, `.featured-heading`, `.featured-retail .empty` không còn chữ trắng trên nền trắng.
- Giữ nguyên `data-testid="featured-products"` (test e2e đang dùng).

## Ràng buộc bắt buộc

- **Bố cục chung và luồng người dùng giữ nguyên**: header, thanh điều hướng, mega menu danh mục, giỏ hàng, thanh toán, đơn hàng, chat — không đụng tới.
- **Không đổi hệ màu**. Chỉ dùng token và màu đã có trong các file CSS hiện tại.
- **Không thêm dependency mới** vào `package.json` (không thêm thư viện icon, carousel, UI kit, CSS framework).
- Giữ nguyên code style từng file (dự án viết dồn dòng ở nhiều chỗ) — **không reformat file, không chạy formatter lên toàn repo**.
- Giữ a11y đang có: `aria-label`, `role="status"`, `role="alert"`, `aria-pressed`, hỗ trợ `prefers-reduced-motion`, focus thấy được.
- Mọi khối mới phải responsive ở 3 mốc đang dùng trong dự án: ≤1100px, ≤760px, ≤650px.
- Không hard-code dữ liệu giả vào template để "cho đẹp" — chỗ nào chưa có dữ liệu thì ẩn khối đó.

## Phạm vi file — chỉ được sửa/tạo trong danh sách này

Được phép:

- `frontend/src/RestApp.vue`
- `frontend/src/views/TechHome.vue`
- `frontend/src/views/VoucherPage.vue` (tạo mới)
- `frontend/src/router/index.js` (chỉ thêm route `/vouchers`)
- `frontend/src/components/HomeHero.vue`, `frontend/src/components/OfferCarousel.vue`
- `frontend/src/components/home/PromoRow.vue`
- `frontend/src/home.css`, `frontend/src/experience.css`, `frontend/src/retail.css`
- `frontend/tests/*.spec.js` — chỉ cập nhật test bị ảnh hưởng trực tiếp bởi 7 việc trên, hoặc thêm test mới

**Nghiêm cấm:**

- Sửa bất cứ file nào ngoài `frontend/` — đặc biệt `src/main/java/`, `src/main/resources/`, `pom.xml`, migration SQL.
- Sửa `src/views/AdminPage.vue` và các component admin (`AdminLayout`, `OfferForm`, `ScopePicker`, `ScopeGroupBrowser`, `ScopeProductList`, `SearchMultiSelect`) — phần upload ảnh trong admin thuộc nhiệm vụ khác.
- Sửa `src/services/api.js`, `src/stores/`, `src/lib/shop.js` (được **đọc và dùng** các hàm có sẵn, không sửa).
- Sửa `src/rest.css`, `src/catalog.css`, `src/admin-layout.css`.
- Đổi hoặc xoá các `data-testid` và `aria-label` đang có.
- Xoá test cho khỏi fail. Test fail nghĩa là bạn đã làm hỏng hành vi — sửa code của bạn.
- Tự ý làm thêm việc ngoài 7 mục trên ("tiện tay dọn code", đổi tên biến, nâng cấp cú pháp, gộp component…).

## Kiểm chứng trước khi báo xong

Chạy và dán kết quả thật:

```
cd frontend
npm run test:unit
npm run build
```

Cả hai phải xanh. Sau đó chạy `npm run dev`, mở trang chủ và `/vouchers`, tự kiểm tra ở 1440px và 390px:

- Sidebar danh mục không còn thanh cuộn.
- Ảnh banner trang chủ không bị ám màu.
- Khối "Gợi ý dành cho bạn" không còn hàng tab, chữ đọc rõ trên nền mới.
- Footer hiển thị đủ 4 tầng, không có link chết.

(E2E Playwright chỉ chạy nếu máy đã cài browser — nếu `npx playwright install` chưa chạy thì ghi rõ là đã bỏ qua, **không tự tải browser**.)

## Báo cáo cuối

Liệt kê file đã sửa/tạo, mô tả ngắn thay đổi từng khối, kết quả `test:unit` + `build`, và những chỗ bạn phải tự quyết (ví dụ chiều cao banner, số cột). Nếu phát hiện việc cần làm nhưng ngoài phạm vi → **ghi ra, không tự ý làm**. Không tự commit, không tự push.
