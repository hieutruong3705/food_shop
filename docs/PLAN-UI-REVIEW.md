# Kế hoạch: menu danh mục, bộ lọc, trang chủ, chân trang và rà soát logic

Cập nhật 2026-09-30. Tài liệu này chỉ phân tích và viết prompt, **chưa sửa code**. Khi bạn báo sửa phần nào thì dán prompt tương ứng ở mục 6.

Phạm vi đã xem: menu danh mục (desktop), trang danh sách sản phẩm và bộ lọc, trang chi tiết, giỏ hàng, cuối trang chủ (ảnh bạn gửi), admin khuyến mãi/voucher. **Chưa xem bằng mắt**: thanh toán, đơn hàng, tài khoản, đăng nhập/đăng ký, chính sách, các trang admin khác ngoài khuyến mãi/voucher. Mục 4 liệt kê cách quét các trang đó.

## 1. Yêu cầu từ ảnh

| # | Yêu cầu | Trạng thái |
| --- | --- | --- |
| R1 | Menu danh mục: có chỗ hiện "Top bán chạy", chỗ "Sản phẩm mới"; muốn hiện **cả hai** | Chưa làm |
| R2 | Thiết kế lại bộ lọc **bảo hành** cùng kiểu với bộ lọc **giá** | Chưa làm |
| R3 | Bỏ gợi ý giá (ba nút "Đến … / … – … / Trên …") dưới bộ lọc giá | Chưa làm |
| R4 | Quét các chỗ giao diện chưa ổn, thiết kế lại | Mục 4 |
| R5 | Trang chủ lướt xuống còn ngắn: thêm sản phẩm hoặc khối nội dung | Mục 3.3 |
| R6 | Chân trang màu xanh | Mục 3.4 |
| R7 | Test lại luồng hãng ↔ danh mục | Mục 5 |
| R8 | Đọc tổng thể, review kiến trúc, tìm lỗi logic, viết kế hoạch | Mục 2 và 6 |

## 2. Phân tích hiện trạng

### 2.1 Vì sao menu lúc "Top bán chạy", lúc "Sản phẩm mới"

`CatalogReadService.menu()` (`src/main/java/com/example/shop/api/service/CatalogReadService.java`) lấy `orderDetails.topSales(categoryId)`: chỉ tính đơn `CONFIRMED/SHIPPING/COMPLETED` đã thanh toán hoặc COD. Nếu danh mục **chưa có đơn nào** thì đổi sang danh sách sản phẩm mới nhất và đặt `rankingSource = "newest"`. `CategoryMenu.vue` đổi tiêu đề theo `rankingSource`. Nên Laptop (có đơn) hiện "Top bán chạy", Điện thoại (chưa có đơn) hiện "Sản phẩm mới". Không phải lỗi hiển thị mà là cách thiết kế "dự phòng"; bạn muốn cả hai luôn hiện.

### 2.2 Hai định nghĩa "bán chạy" khác nhau

- Menu dùng `topSales` (đếm từ chi tiết đơn, có lọc trạng thái).
- Trang danh sách sắp xếp `popular` và "Thiết bị nổi bật" ở trang chủ dùng cột `products.sold`, tăng ngay khi đặt hàng (kể cả đơn `PENDING`, VNPay chưa trả tiền) và giảm khi hủy.

Hai nguồn cho hai thứ tự khác nhau cho cùng một chữ "bán chạy". Cần chọn một định nghĩa (khuyến nghị: theo đơn đã xác nhận, như `topSales`) và dùng cho cả menu, sắp xếp, trang chủ.

### 2.3 Bộ lọc giá và bảo hành

`CatalogPage.vue` (dòng khoảng 55–58):

- Giá: một thanh trượt một đầu "Tối đa", hai ô số "Giá từ/Giá đến", nút "Áp dụng giá", cộng ba nút gợi ý chia ba theo giá tối đa. Bấm gợi ý đổ số lẻ (ví dụ 793333) vào ô, thanh trượt không phản ánh khoảng đã chọn. Thanh trượt bắt đầu từ 0 nên phần lớn hành trình trượt nằm dưới giá thấp nhất.
- Bảo hành: thanh trượt hai đầu (`RangeSlider.vue`), tự áp dụng khi thả, không ô số, không nút. Nếu mọi sản phẩm cùng số tháng chỉ hiện chữ "Tất cả sản phẩm: 24 tháng".

Hai bộ lọc cùng loại (khoảng số) nhưng hai cách dùng khác nhau, giá thì có ba lối vào chồng chéo.

### 2.4 Trang chủ ngắn

`TechHome.vue` hiện gồm: banner + ưu đãi, 4 lợi ích, lưới danh mục, "Thiết bị nổi bật" (8 sản phẩm, có tab), khối "Không gian của bạn", "Sản phẩm mới" (8), danh sách banner, chân trang. Tổng cộng chỉ hai lưới sản phẩm và không có khối khuyến mãi, thương hiệu, đánh giá. Ảnh của bạn cho thấy cuối trang còn ba banner thử nghiệm (`xdfcvsxd`, `gsdffgsd`) dùng ảnh không liên quan: đó là **dữ liệu trong DB**, cần dọn ở admin Banner, không phải lỗi code.

### 2.5 Chân trang

Màu chân trang định nghĩa hai lần: `rest.css` (`#eef1e8`) và `retail.css` (`#fff`). `retail.css` nạp sau nên thắng, kết quả là nền trắng. Đổi màu phải sửa ở `retail.css` (hoặc gom về một chỗ) và kiểm tra màu chữ, màu liên kết, đường kẻ.

## 3. Thiết kế đề xuất

### 3.1 Menu danh mục: hiện cả hai

Backend `GET /api/v1/catalog/menu?categoryId=` trả thêm khóa mới, giữ khóa cũ để không vỡ client và test:

- `bestSellers`: giữ nguyên, nhưng **không** đổi sang sản phẩm mới khi rỗng; rỗng thì trả `[]`.
- `newest`: 5 sản phẩm mới nhất còn bán của danh mục (còn hàng trước).
- `rankingSource` giữ để tương thích, đánh dấu lỗi thời.

Giao diện `CategoryMenu.vue`: cột trái là danh sách danh mục, phần chi tiết chia hai hàng hoặc hai cột: "Top bán chạy" (có "Đã bán N") và "Sản phẩm mới", hàng dưới là "Chọn thương hiệu" và "Mức giá". Danh mục chưa có đơn thì cột bán chạy hiện "Chưa có dữ liệu bán hàng" thay vì trống hoặc bị thay thế. Mỗi dòng có ảnh nhỏ và nhãn giảm giá nếu có. Trên mobile (≤ 760 px) xếp dọc, giữ hành vi hiện tại và sửa lỗi nút mũi tên bị đè (xem `docs/FIX-PROMPTS.md` mục 2.1).

### 3.2 Bộ lọc giá và bảo hành thống nhất

Một component dùng chung `FilterRange.vue` (bọc `RangeSlider.vue`):

- Đầu khối: hai "viên" giá trị hiện khoảng đang chọn (ví dụ `790.000 ₫ — 16.290.000 ₫`, `12 — 36 tháng`).
- Thanh trượt hai đầu, cận lấy đúng `facets.min/max` (giá không bắt đầu từ 0). Bước giá 10.000 ₫, bước bảo hành 1 tháng.
- Hai ô số đồng bộ hai chiều với thanh trượt.
- Tự áp dụng: khi thả thanh trượt, khi Enter hoặc rời ô số, có debounce 400 ms. **Bỏ** nút "Áp dụng giá" và ba nút gợi ý.
- Khoảng trùng cận thì không đưa tham số lên URL; chip lọc hiện "Giá 790.000 ₫ – 5.000.000 ₫" và "Bảo hành 24–36 tháng", bấm × để xóa.
- Nếu min = max (mọi sản phẩm bằng nhau) hiện một dòng chữ, không hiện thanh trượt.
- Giữ tên trợ năng `Bảo hành từ`, `Bảo hành đến`, thêm `Giá từ`, `Giá đến` cho thanh trượt (test hiện có dùng các tên bảo hành). Ô số đặt tên `Giá từ (nhập số)`, `Giá đến (nhập số)` để không trùng.
- Form giá trong menu danh mục (nút `Áp dụng giá`, `catalog.spec.js` dùng) **giữ nguyên**: bạn chỉ yêu cầu bỏ phần gợi ý ở sidebar.

### 3.3 Trang chủ dài hơn, có nội dung

Thêm khối theo thứ tự (mỗi khối ẩn nếu không có dữ liệu). Khuyến nghị một endpoint gộp `GET /api/v1/home` có cache ngắn (30–60 giây) để tránh nhiều yêu cầu nối tiếp:

1. **Ưu đãi đang chạy**: hàng sản phẩm đang giảm giá của khuyến mãi tỉ lệ cao nhất, có đồng hồ đếm ngược đến `endDate`, nút "Xem tất cả" tới `/products?promotionId=`. Dữ liệu sẵn có (`/offers`, `/products?promotionId=`).
2. **Thương hiệu nổi bật**: dải logo hãng (`/brands`, dùng `BrandMark.vue`), bấm vào lọc theo hãng.
3. **Bán chạy nhất**: 8 sản phẩm theo định nghĩa thống nhất ở 2.2 (cần truy vấn tổng thể, không theo danh mục).
4. **Mỗi danh mục một hàng**: ba danh mục có nhiều sản phẩm nhất, mỗi hàng 4 sản phẩm và liên kết "Xem tất cả".
5. **Khách hàng nói gì**: 3–4 đánh giá 4–5 sao mới nhất (đánh giá chỉ tạo được sau đơn `COMPLETED`, nên đáng tin). Cần endpoint `GET /api/v1/reviews/featured`.
6. **Sản phẩm bạn vừa xem**: đọc từ `localStorage`, không cần backend.
7. Giữ "Thiết bị nổi bật", "Sản phẩm mới", banner. Đặt banner cuối, giới hạn 3 cái và chiều cao đều.

Mỗi khối là một component riêng trong `frontend/src/components/home/` để `TechHome.vue` không phình to.

### 3.4 Chân trang xanh

Nền `#073f37` (cùng màu thanh bên admin), chữ `#e4f3ed`, liên kết trắng khi hover, tiêu đề cột `#95c7b8`, đường kẻ `rgba(255,255,255,.14)`, ô "COD" nền `#0d5a4d`. Thêm một hàng dải trên chân trang (thanh ngôn ngữ/liên hệ) và cột liên hệ; kiểm tra độ tương phản ≥ 4.5:1. Sửa ở `retail.css`, xóa định nghĩa trùng ở `rest.css` để không còn hai nguồn.

## 4. Rà soát giao diện: cách quét các trang chưa xem

Chụp ảnh từng trang ở 1400 px, 768 px và 390 px, kèm console và tràn ngang. Danh sách: `/`, `/products` (có và không lọc), `/products/:id` (có/không phiên bản), `/cart`, `/checkout`, `/orders`, `/orders/:id`, `/account`, `/login`, `/register`, `/forgot-password`, `/policies`, 404, 403, và các trang admin: tổng quan, sản phẩm, danh mục, thương hiệu, banner, chính sách, người dùng, đánh giá, đơn hàng. Với mỗi trang ghi: tràn ngang, chữ bị cắt, tương phản, trạng thái trống/lỗi/đang tải, phím Tab. Kết quả đưa vào bảng "trang / vấn đề / mức độ / đề xuất" rồi mới sửa.

Nghi ngờ đã thấy: cả `rest.css` lẫn `retail.css` ghi đè nhau (chân trang, `.price`, `.badge`), dễ gây lỗi khi thêm class trùng tên; các form admin ngoài khuyến mãi/voucher còn kiểu cũ (xem `docs/FIX-PROMPTS.md` mục 2.4).

## 5. Luồng hãng ↔ danh mục: ma trận kiểm thử

Quy tắc hiện có: sản phẩm chỉ chọn được hãng đã gán cho danh mục của nó; không bỏ gán hãng khi còn sản phẩm dùng; xóa danh mục/hãng bị chặn nếu đang được voucher, khuyến mãi hoặc sản phẩm dùng; phạm vi ưu đãi theo danh mục/hãng/sản phẩm (đợt trước).

| # | Tình huống | Kỳ vọng | Đã có test? |
| --- | --- | --- | --- |
| 1 | Gán hãng A, B cho danh mục X; tạo sản phẩm hãng B | Được | Có (`scopes.spec.js`) |
| 2 | Tạo sản phẩm danh mục X với hãng C chưa gán | 400, thông báo nêu cách gán | Có (`ScopeApiTests`) |
| 3 | Đổi danh mục của sản phẩm sang Y | Ô hãng bị xóa, chỉ liệt kê hãng của Y | Có (`scopes.spec.js`) |
| 4 | Bỏ gán hãng B khi còn sản phẩm hãng B trong X | 400 nêu hãng | Có (`ScopeApiTests`) |
| 5 | Bỏ gán tất cả hãng khi còn sản phẩm | 400 | Có |
| 6 | Xóa danh mục đang có khuyến mãi/voucher | 400 nêu tên khuyến mãi/mã | Có (`OfferScopeTests`) |
| 7 | Xóa hãng đang được gán cho danh mục | 400 nêu tên danh mục | Có |
| 8 | Xóa hãng đang có sản phẩm | 400 | Có (`BrandCategoryFlowTests`) |
| 9 | Ngừng bán mọi sản phẩm của hãng: hãng còn hiện ở bộ lọc/menu? | Hiện với số lượng 0 ở cả bộ lọc và menu (đã chốt) | Có (`BrandCategoryFlowTests`) |
| 10 | Đổi tên danh mục/hãng: khuyến mãi theo nhóm vẫn đúng | Đúng (theo ID) | Có (`BrandCategoryFlowTests`) |
| 11 | Khuyến mãi theo hãng, thêm sản phẩm mới của hãng | Sản phẩm mới tự được giảm | Có (`BrandCategoryFlowTests`) |
| 12 | Khuyến mãi theo danh mục, chuyển sản phẩm sang danh mục khác | Mất giảm giá | Có (`BrandCategoryFlowTests`) |
| 13 | Hai khuyến mãi chồng nhau cùng sản phẩm | Lấy tỉ lệ cao nhất | Có (`BrandCategoryFlowTests`) |
| 14 | Voucher theo hãng, giỏ có hãng khác | Chỉ giảm dòng cùng hãng | Có (`ScopeApiTests`) |
| 15 | Menu danh mục với danh mục không có hãng | Thông báo "Danh mục chưa gán thương hiệu" | Có (`BrandCategoryFlowTests`, phần API) |

## 6. Vấn đề logic và kiến trúc

Mức: **Cao** = sai dữ liệu hoặc tiền; **TB** = sai hành vi, có cách chữa; **Thấp** = dọn dẹp/quan sát.

| ID | Mức | Vấn đề | Bằng chứng | Đề xuất |
| --- | --- | --- | --- | --- |
| L1 | Cao | **Voucher không được hoàn lượt dùng khi đơn bị hủy hoặc hết hạn thanh toán.** `usedCount` chỉ tăng ở `checkout`, không nơi nào giảm; voucher giới hạn lượt sẽ cạn dần dù đơn không thành | `CommerceService.checkout` tăng `usedCount` (tìm `setUsedCount` chỉ thấy một chỗ ghi); `status()` và `SandboxPaymentService.release` chỉ hoàn kho qua `variants.restore` | Khi hủy/hết hạn: tìm voucher theo `order.voucherCode` (khóa hàng), giảm `usedCount` không dưới 0; test |
| L2 | TB | Hai định nghĩa "bán chạy" (mục 2.2) | `topSales` và `products.sold` | Thống nhất, có test |
| L3 | TB | Menu chỉ hiện một trong hai danh sách | mục 2.1 | mục 3.1 |
| L4 | TB | Voucher tính phần trăm trên giá đã giảm bởi khuyến mãi (cộng dồn); miễn phí vận chuyển xét trên tạm tính sau khuyến mãi nhưng trước voucher | `CommerceService.totals` | Chốt quy tắc kinh doanh, ghi vào tài liệu và hiển thị ở giỏ hàng để khách thấy |
| L5 | TB | Lọc danh mục ở danh sách voucher bỏ sót voucher chọn lẻ sản phẩm | `AdminSupport.matching` | `docs/FIX-PROMPTS.md` mục 2.5 |
| L6 | TB | Ưu đãi cũ chọn cả danh mục lẫn hãng bị đóng băng thành danh sách sản phẩm sau V11 | `V11__offer_scope_by_group.sql` | Ghi chú cho người quản trị; nếu muốn, tạo lại thủ công bằng bộ chọn mới |
| L7 | TB | Hai cờ ẩn sản phẩm (`active` và `technologyCatalog`) gây khó hiểu; bộ đếm `productCount` của danh mục đếm cả sản phẩm ẩn (`@Formula`) trong khi menu chỉ tính sản phẩm bán | `Category.productCount` và `facets` | Gộp về một cờ hoặc đặt tên rõ; đếm cùng một quy tắc |
| L8 | TB | Khuyến mãi chồng nhau không cảnh báo trong admin | `PromotionPricing.sale` chọn tỉ lệ cao nhất | Khi lưu, cảnh báo nếu trùng phạm vi và thời gian (không chặn) |
| L9 | Thấp | Cột `cart_items.price` được ghi khi thêm nhưng giỏ tính lại giá mỗi lần, cột chỉ còn là dữ liệu chết | `CommerceService.add` | Bỏ ghi hoặc dùng làm "giá khi thêm" để báo giá thay đổi |
| L10 | Thấp | Tìm kiếm chỉ theo tên sản phẩm, không theo SKU, thương hiệu, danh mục | `CatalogReadService` (`like` trên `name`) | Mở rộng tìm kiếm, có test |
| L11 | Thấp | `spring.jpa.open-in-view=true`; mã Java có nhiều hàm một dòng dài; thư mục `legacy/`, bộ TypeScript cũ còn nằm trong cây | `application.properties`, `frontend/src/main.ts` | P8 trong `docs/CODEX-HANDOFF.md` |
| L12 | Thấp | Đơn COD `PENDING` không bao giờ hết hạn (chỉ VNPay có tác vụ hết hạn) | `PaymentExpiryJob` | Quyết định chính sách (ví dụ tự hủy sau N ngày không xác nhận) |

Điểm tốt cần giữ: khóa hàng theo thứ tự ID khi đặt hàng, khóa idempotency cho `checkout`, giá và tên sản phẩm được chụp vào chi tiết đơn, đánh giá chỉ sau đơn hoàn thành, một quy tắc phạm vi ưu đãi dùng chung (`OfferScopeRule`), giới hạn kích thước và số lượng ở mọi API đầu vào.

## 7. Kế hoạch triển khai và prompt

Thứ tự khuyến nghị: **A** (logic, tiền) → **B** (menu) → **C** (bộ lọc) → **D** (trang chủ và chân trang) → **E** (quét giao diện) → **F** (kiểm thử hãng/danh mục). A độc lập; B, C, D có thể làm song song trên các nhánh riêng; E sau khi D xong.

### Prompt A: sửa logic voucher và thống nhất "bán chạy"

```
Bối cảnh: TechShop tại D:\food_shop (Spring Boot 3.5 / Java 17, Vue 3, MySQL + Flyway). Đọc docs/PLAN-UI-REVIEW.md mục 2.2 và 6 (L1, L2). Không sửa migration cũ. Giữ kiểu xuống dòng từng file. Đừng reset/stash.
1. L1: khi đơn bị hủy (CommerceService.status khi next=CANCELLED) và khi đơn VNPay hết hạn (SandboxPaymentService.expire), nếu order.voucherCode không rỗng thì khóa voucher theo mã (VoucherRepository.lockByCode) và giảm usedCount đi 1, không xuống dưới 0. Mỗi đơn chỉ được hoàn một lần (kiểm tra trạng thái trước khi đổi để chuyển CANCELLED hai lần không hoàn hai lần). Nếu voucher đã bị xóa thì bỏ qua, không lỗi.
2. Viết test Java: đặt hàng có voucher usageLimit=1, hủy đơn, voucher dùng lại được; hủy hai lần chỉ hoàn một lần; VNPay hết hạn hoàn lượt.
3. L2: chọn một định nghĩa "bán chạy" là tổng số lượng trong đơn CONFIRMED/SHIPPING/COMPLETED đã thanh toán hoặc COD (như OrderDetailRepository.topSales). Thêm truy vấn tổng thể (không theo danh mục) và dùng cho sắp xếp sort=popular của /products cùng khối "Bán chạy nhất" mới; không dùng products.sold để xếp hạng nữa. Giữ cột sold. Cập nhật test liên quan (CatalogQueryCountTests: đo số query GET /products?size=12 trước và sau, không được tăng).
Xong khi: .\mvnw.cmd test đạt; ghi kết quả vào docs/ARCHITECTURE-IMPLEMENTATION.md.
```

### Prompt B: menu danh mục hiện cả "Top bán chạy" và "Sản phẩm mới"

```
Bối cảnh: TechShop tại D:\food_shop. Đọc docs/PLAN-UI-REVIEW.md mục 2.1 và 3.1.
Backend: CatalogReadService.menu(): giữ khóa bestSellers nhưng trả [] khi không có đơn (không thay bằng sản phẩm mới); thêm khóa newest: 5 sản phẩm mới nhất còn bán của danh mục (còn hàng trước), cùng định dạng {product, units:0}; giữ rankingSource để tương thích. Cập nhật ExperienceApiTests.
Frontend: frontend/src/components/CategoryMenu.vue hiện hai khối "Top bán chạy" (có "Đã bán N", rỗng thì "Chưa có dữ liệu bán hàng") và "Sản phẩm mới", mỗi dòng có ảnh nhỏ và nhãn giảm giá nếu discountRate>0. Bố cục desktop: hàng trên hai khối sản phẩm, hàng dưới Thương hiệu và Mức giá; mobile xếp dọc và sửa lỗi nút mũi tên bị .mega-detail đè (docs/FIX-PROMPTS.md 2.1). Giữ nút "Áp dụng giá" trong menu.
Xong khi: catalog.spec.js đạt 5 lần liên tiếp, ExperienceApiTests đạt, ảnh chụp menu ở 1400 px và 390 px cho danh mục có đơn và chưa có đơn.
```

### Prompt C: bộ lọc giá và bảo hành thống nhất

```
Bối cảnh: TechShop tại D:\food_shop. Đọc docs/PLAN-UI-REVIEW.md mục 2.3 và 3.2. Sửa frontend/src/views/CatalogPage.vue và frontend/src/components/RangeSlider.vue.
Làm component frontend/src/components/FilterRange.vue dùng chung cho Mức giá và Bảo hành: hai viên giá trị, thanh trượt hai đầu với cận lấy từ facets (giá không bắt đầu từ 0; bước giá 10.000, bảo hành 1), hai ô số đồng bộ hai chiều, tự áp dụng khi thả thanh trượt/Enter/rời ô số (debounce 400 ms), min=max thì chỉ hiện chữ. XÓA nút "Áp dụng giá" và ba nút gợi ý (class price-presets, hàm priceRange) ở sidebar, và ô "Tối đa" một đầu. Khoảng trùng cận thì không đưa tham số lên URL. Chip lọc hiện "Giá A – B", "Bảo hành A–B tháng", bấm × xóa. Tên trợ năng thanh trượt: "Giá từ", "Giá đến", "Bảo hành từ", "Bảo hành đến"; ô số "Giá từ (nhập số)"...
Cập nhật frontend/tests (scopes.spec.js dùng slider "Bảo hành từ/đến"; đừng làm hỏng), thêm ca E2E: kéo giá thì URL có minPrice/maxPrice, tải lại giữ nguyên, xóa chip trả về đủ sản phẩm, ở 390 px không tràn ngang.
Xong khi: npm.cmd --prefix frontend run build và E2E đạt; ảnh chụp sidebar desktop và mobile.
```

### Prompt D: trang chủ dài hơn và chân trang xanh

```
Bối cảnh: TechShop tại D:\food_shop. Đọc docs/PLAN-UI-REVIEW.md mục 3.3, 3.4 và 2.5.
1. Backend: GET /api/v1/home (công khai, cache 30–60 giây) trả: promotionRow {promotion, products[8]} của khuyến mãi tỉ lệ cao nhất còn sản phẩm bán; brands[] (id, name, logoUrl, count); bestSellers[8] (theo định nghĩa ở Prompt A); categoryRows[3] {category, products[4]}; và GET /api/v1/reviews/featured (4 đánh giá 4–5 sao mới nhất, ẩn email). Test Java cho từng khóa và cho trường hợp rỗng.
2. Frontend: tách frontend/src/components/home/ (PromoRow, BrandStrip, BestSellers, CategoryRows, ReviewStrip, RecentlyViewed) và ghép vào TechHome.vue giữa "Bạn đang tìm gì?" và banner; mỗi khối ẩn nếu rỗng, có trạng thái đang tải và lỗi; RecentlyViewed đọc localStorage (ghi ở ProductPage.vue, tối đa 8). Banner ở cuối giới hạn 3 cái, cao đều.
3. Chân trang: theo mục 3.4, sửa ở retail.css và xóa định nghĩa trùng ở rest.css; thêm hàng liên hệ; kiểm tra tương phản.
Không dùng dữ liệu giả trong mã: khối nào chưa có dữ liệu thì ẩn. Xong khi: E2E đạt (retail.spec.js, shop.spec.js), ảnh chụp trang chủ đầy đủ ở 1400/768/390 px, chiều cao trang chủ tăng rõ rệt khi DB demo.
```

### Prompt E: quét giao diện toàn bộ

```
Bối cảnh: TechShop tại D:\food_shop. Đọc docs/PLAN-UI-REVIEW.md mục 4. CHỈ ĐIỀU TRA, chưa sửa. Chạy backend E2E cô lập (cổng 18080, xem scripts/run-e2e.mjs; không đụng backend cổng 8080). Viết script Playwright chụp từng trang trong danh sách ở mục 4 ở 1400, 768 và 390 px, ghi console error, tràn ngang, ảnh hỏng, phần tử không truy cập được bằng Tab. Xuất docs/UI-AUDIT.md có bảng "trang / vấn đề / mức độ / đề xuất" và thư mục ảnh data/ui-audit/ (data/ đã bị git ignore). Sau đó dừng để tôi chọn mục cần sửa.
```

### Prompt F: kiểm thử luồng hãng ↔ danh mục

```
Bối cảnh: TechShop tại D:\food_shop. Đọc docs/PLAN-UI-REVIEW.md mục 5. Với mỗi dòng "Chưa" trong ma trận (8, 9, 10, 11, 12, 15) viết test: Java (MockMvc, xem OfferScopeTests/ScopeApiTests làm mẫu) cho quy tắc backend, Playwright (frontend/tests/scopes.spec.js hoặc file mới) cho phần giao diện. Dòng 9: chốt hành vi (khuyến nghị: hãng còn hiện ở bộ lọc với số 0, ẩn khỏi menu nếu không có sản phẩm bán) và ghi vào docs/CATEGORY-BRAND-SCOPES.md. Nếu test phát hiện sai, ghi lại và báo, đừng sửa im lặng.
Xong khi: mọi test mới đạt hoặc có báo cáo lỗi rõ ràng; .\mvnw.cmd test và E2E đạt.
```

## 8. Rủi ro và lưu ý

- Sửa CSS toàn cục (`rest.css`, `retail.css`) ảnh hưởng nhiều trang: luôn chạy E2E và chụp lại ảnh.
- `catalog.spec.js` đang dễ vỡ (xem `docs/FIX-PROMPTS.md` 2.1); làm B trước hoặc cùng lúc.
- Thay đổi menu không được làm hỏng `ExperienceApiTests` (đang kiểm `rankingSource`); Prompt B giữ khóa này.
- Backend cổng 8080 của bạn chạy bản cũ; sau khi sửa cần khởi động lại và sao lưu DB trước khi chạy V11 (xem `docs/FIX-PROMPTS.md` 2.2).
