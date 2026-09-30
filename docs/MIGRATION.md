# TechShop — trạng thái chuyển đổi

## Cập nhật 2026-09-30 — nguồn thời gian khuyến mãi (P2, kiến trúc H5)

**Tái hiện (trước khi sửa).** Dùng MySQL 8.0.44 tạm ở cổng riêng với `default-time-zone='+00:00'` (đúng mặc định của `mysql:8.0` trong Docker), Flyway V1–V7, JDBC `serverTimezone=Asia/Ho_Chi_Minh`, chạy `PromotionClockTests` với JVM UTC và với JVM `Asia/Ho_Chi_Minh`. Cả hai tổ hợp đều lỗi cùng một chỗ: khuyến mãi bắt đầu 1 phút trước (giờ VN) không có giá sale (`discountedPrice` 1.000.000 thay vì 900.000), trong khi `/api/v1/offers` và bộ lọc `promotionId` (dùng giờ JVM) coi khuyến mãi đang chạy. Nguyên nhân: giá sale là `@Formula` so `start_date`/`end_date` (giờ VN đã lưu) với `current_timestamp` của phiên MySQL (UTC), lệch 7 giờ; khuyến mãi đã kết thúc 3 giờ trước vẫn bị coi là còn hiệu lực. Với MySQL `+07:00` hoặc H2 thì không lỗi, nên máy local không thấy.

**Đã sửa.**
- `config/TimeConfig`: bean `java.time.Clock` (múi giờ `Asia/Ho_Chi_Minh`) dùng cho `/offers`, voucher, bộ lọc `promotionId`, hết hạn thanh toán, đơn hoàn tất và ngày mặc định của dashboard.
- Với URL `jdbc:mysql:`/`jdbc:mariadb:`, ứng dụng tự đặt `hibernate.jdbc.time_zone=Asia/Ho_Chi_Minh` (ghi và đọc mốc thời gian theo giờ VN, không phụ thuộc múi giờ JVM) và chạy `SET time_zone = '+07:00'` cho mọi kết nối (`current_timestamp` cũng là giờ VN, không phụ thuộc cấu hình máy chủ). Không đụng H2 nên test và E2E giữ nguyên.
- `compose.yaml`: `TZ=Asia/Ho_Chi_Minh` cho cả hai service và `--default-time-zone=+07:00` cho MySQL.

**Sau khi sửa.** `PromotionClockTests` đạt trên MySQL `+00:00` với JVM UTC và JVM giờ VN, và trên H2; 47 test (Commerce, Admin, Payment, Scope, Experience, RestArchitecture, PromotionClock, PaymentExpiryJob) đạt trên MySQL `+00:00` với JVM UTC. `FixedClockTests` cố định Clock ở 23:59 và 00:01 giờ VN quanh mốc 00:00 để kiểm tra banner ưu đãi, voucher và bộ lọc `promotionId`.

**Còn lại.** Giá sale vẫn do `@Formula` tính bằng giờ của database (đã cố định bằng `SET time_zone`); nó chưa dùng `Clock`. P7 thay `@Formula` bằng `PromotionPricing` tính trong Java để giá, banner và voucher dùng cùng một `Clock`. Dữ liệu cũ không cần chuyển đổi: khi tái hiện, mốc bắt đầu luôn được lưu theo giờ VN dù JVM chạy UTC hay giờ VN, vì JDBC URL mặc định có `serverTimezone=Asia/Ho_Chi_Minh`. Nếu môi trường nào dùng JDBC URL không có tham số này, hãy đối chiếu vài khuyến mãi/voucher đang chạy sau khi triển khai.

## Cập nhật 2026-09-30 — gán hãng và phạm vi ưu đãi

Theo yêu cầu mới, Category–Brand được quản lý rõ bằng bảng `category_brands` và form chọn nhiều hãng. V6 backfill từ Product, thêm scope ALL/CATEGORY/BRAND/PRODUCT cho cả Promotion/Voucher; giữ giá trị ưu đãi cũ bằng chuyển đổi/tách phạm vi tương đương. Bộ lọc bỏ Phiên bản/Kết nối, dùng bảo hành dạng thanh kéo theo tháng. Xem [CATEGORY-BRAND-SCOPES.md](CATEGORY-BRAND-SCOPES.md) cho file, migration, API, quy tắc và test. Mục bên dưới ghi lại kiến trúc trước yêu cầu mới này.

## Cập nhật 2026-09-30 — rút gọn menu và tách admin

- Mega menu còn bán chạy/mới, thương hiệu động và giá. Product là nguồn quan hệ category-brand; không thêm bảng dư thừa.
- Bỏ pause/play, giữ tự chuyển banner/khuyến mãi/voucher 4,5 giây và mũi tên/reset timer.
- Review có API tổng hợp AVG/count/canReview và giao diện đúng quyền mua hàng. Admin order có thao tác nhanh với allowedStatuses từ cùng quy tắc backend, giữ bộ lọc khi cập nhật.
- Layout admin riêng gồm sidebar/topbar/content; giữ auth/role/CSRF. Không đổi schema/Flyway.
- Backend 40 đạt, 1 skip; bộ Commerce 17 ca chạy lại đạt sau thêm assertion. Browser 13/13 đạt; build/package đạt. File, endpoint và cách test: [ADMIN-REVIEW-REFINEMENT.md](ADMIN-REVIEW-REFINEMENT.md).

## Cập nhật 2026-09-30 — chatbot Gemini thật

- Thêm `GeminiChatService`, cấu hình backend bằng GEMINI_API_KEY hoặc `data/chat-local.properties` (Git ignore). Giữ endpoint cũ, thêm history/provider và GET status. Không đổi schema/dependency.
- Có timeout, giới hạn đồng thời/session, phản hồi lỗi đã lọc thông tin nhạy cảm; UI giữ câu hỏi để gửi lại, gửi tối đa 8 tin trước đó, không gửi lượt thất bại.
- Khóa được cung cấp dạng văn bản đã được Google xác thực; gọi thật qua backend trả HTTP 200 với câu trả lời Gemini. 10 test Java, 2 bài browser, build/package đạt. Chi tiết [CHATBOT.md](CHATBOT.md); log `data/gemini-tests.log`, `gemini-e2e.log`, `gemini-smoke.log`.

## Cập nhật cuối 2026-09-29 — menu bán chạy, carousel, ưu đãi và chat

- Bổ sung theo yêu cầu tiếp: voucher và khuyến mãi luân phiên 4,5 giây, nút trước/sau/tạm dừng, dừng khi hover/focus; trả toàn bộ mã đủ điều kiện. Khuyến mãi còn hạn nhưng các sản phẩm áp dụng đã ngừng bán vẫn hiện mức giảm cùng thông báo và không dẫn đến danh sách trống. Dữ liệu MySQL xác nhận TECH10 10%, GDFFG 5%, chương trình Orbit 5%; không sửa các giá trị này. Xem `OfferCarousel.vue`, `OffersApiController.java` và API field `hasAvailableProducts`.
- Lượt bổ sung: 4/4 ExperienceApiTests đạt (`data/offers-tests.log`); browser auto/manual/voucher/khuyến mãi/chat desktop-mobile đạt (`data/offers-e2e.log`); build/package đạt (`data/offers-build.log`, `data/offers-package.log`). JAR mới chạy port 8080, log `data/offers-server.log`. Fixture kiểm thử đã được dọn; không thêm migration.

- Hoàn tất menu DB với top 10 theo tổng số lượng từ đơn hợp lệ, hãng/giá/thông số đúng danh mục và giữ query. Banner DB tự chuyển 4,5 giây, có mũi tên/dot/tạm dừng; hai ô ưu đãi lấy promotion/voucher thật. TechShop AI dùng mock backend `/api/chat`.
- Sửa Bán lại/Ngừng bán, hết hàng xuống cuối trước phân trang, trạng thái giỏ hàng và các trường hợp bộ lọc. Không thêm migration; giữ Flyway và `ddl-auto=validate`.
- Backend: 35 test đạt, 1 migration test skip theo cấu hình, tổng hợp bộ 33 test cũ và 3 ExperienceApiTests mới. Log `data/experience-tests.log`, `data/experience-new-tests.log`, `data/experience-package-final.log`.
- Browser: toàn bộ 12 ca đạt trong `data/experience-e2e.log`. Sau chỉnh cuối, ca danh mục/query đạt trong `data/experience-final-e2e.log`; ca banner phát hiện lớp template làm ẩn nút, đã sửa và chạy lại đạt trong `data/experience-carousel-retest.log` (gồm desktop/mobile, auto/manual, voucher, chat). Build/package cuối đạt: `data/experience-build-final.log`, `data/experience-package-ui-final.log`.
- Đã ẩn 27 fixture sản phẩm xác định chắc chắn qua hai lượt, có backup `data/backups/test-fixtures-1790699169188.json` và `test-fixtures-1790700159088.json`; lần xem trước cuối còn 0 fixture khớp. Fixture banner/voucher của ca thất bại đã sao lưu và dọn riêng, các ca thành công tự dọn.
- Bản JAR cuối chạy tại http://127.0.0.1:8080 với MySQL techshop. Chi tiết file, endpoint, query, phạm vi rà soát và cách test: [TECHSHOP-COMPLETION.md](TECHSHOP-COMPLETION.md).

## Cập nhật 2026-09-29 — icon, mega menu và bộ lọc theo danh mục

- Danh mục dùng ảnh trong database làm icon tại header và trang chủ; hãng có logo ở menu và bảng lọc. Admin tải ảnh/xem trước/lưu qua chức năng upload hiện có; không cần sửa schema. Menu mở rộng có hãng, giá, tồn kho và thông số; hỗ trợ rê chuột, nút mũi tên, Escape và đóng bên ngoài.
- `GET /catalog/facets` cung cấp nhóm thông số theo danh mục, hãng kèm logo và số lượng sản phẩm đang kinh doanh. Lọc nhiều hãng, còn hàng, giá và `spec.{tên}` thực hiện tại database trước phân trang; OR trong nhóm, AND giữa các nhóm. Bộ lọc được giữ trong URL. Điện thoại có nút mở/thu gọn.
- Hướng dẫn quản lý: [CATALOG-UI.md](CATALOG-UI.md). Không tự thêm thông số phần cứng hay logo thương hiệu vào dữ liệu thật; quản trị viên cập nhật thông số/ảnh đúng cho từng sản phẩm/hãng.
- Kiểm chứng backend: các test cũ đạt; test facet mới ban đầu thiếu `warrantyMonths` trong fixture, đã sửa và cả 7 AdminApiTests đạt (bao gồm facet/count/phân trang/ảnh/stock). Tổng cộng 30 test backend đã đạt, 1 test migration bỏ qua theo cấu hình. Log `data/catalog-api-test.log`, `data/catalog-package.log`.
- Browser: 7 ca cũ đạt trên MySQL/JAR; 2 ca chạy trước lúc server sẵn sàng được chạy lại, và selector “Áp dụng” được đổi sang exact do thêm nút “Áp dụng giá”. Lượt cuối 3/3 ca menu/icon/logo/bộ lọc/catalog desktop-mobile đạt trên JAR 8080 (`data/catalog-final-e2e.log`); tổng cộng 10 ca được xác nhận. Test icon/logo thực sự tải PNG trong admin, lưu rồi xác nhận API/ảnh hiện ở cửa hàng, kiểm tra multi-select và reload. Fixture sản phẩm đã ngừng hiển thị sau kiểm thử.
- Frontend build và JAR package cuối đạt (`data/catalog-build.log`, `data/catalog-package-final.log`). JAR đang chạy tại http://127.0.0.1:8080 với MySQL techshop (`data/catalog-server-final.log`). Ảnh minh chứng: `frontend/test-results/category-mega-menu.png`, `catalog-filters-desktop.png`, `catalog-filters-mobile.png`.

## Cập nhật 2026-09-29 — giao diện bán lẻ TechShop

- Thiết kế tham khảo https://hoanghamobile.com/: thanh dịch vụ, tìm kiếm nổi bật, điều hướng xanh ngọc, danh mục bên trái, banner chuyển bằng nút, khối sản phẩm và giá giảm màu đỏ. Giữ thương hiệu TechShop và dùng ảnh sẵn có của dự án.
- Trang chủ có danh mục từ API, bộ lọc thiết bị nổi bật, sản phẩm mới và banner từ database. Nút banner/tìm kiếm/danh mục dẫn tới bộ lọc catalog thật. Phần trăm giảm tính từ giá API.
- Đồng bộ header/footer, thẻ sản phẩm, trang catalog/chi tiết và các thành phần form; tăng chữ trên điện thoại. CSS mới `frontend/src/retail.css` chỉ được nạp trong REST entry, giữ scaffold TypeScript riêng.
- Bổ sung `frontend/tests/retail.spec.js` kiểm tra chuyển banner, lọc sản phẩm, menu điện thoại và tìm kiếm; kiểm tra không tràn ngang ở 360, 390, 768, 1440 px.
- Frontend build và Maven package đạt. JAR chứa giao diện mới chạy trên MySQL `techshop` tại http://127.0.0.1:8080. Log: `data/retail-build.log`, `data/retail-package.log`, `data/retail-server.log`; kiểm thử bản JAR: `data/retail-e2e-final.log`.
- Kiểm chứng cuối: 8 ca đạt trong lượt đầy đủ; ca COD gặp laptop demo đã hết hàng do các lượt kiểm thử trước. Sửa fixture chọn laptop còn tối thiểu 2 đơn vị (không sửa tồn kho), chạy lại COD/fulfillment/review đạt trong `data/retail-cod-retest.log`. Tổng cộng 9 ca đã xác nhận đạt; ảnh desktop/mobile và tùy chọn lưu ở `data/retail-preview/`. Không thay đổi logic backend trong lần thiết kế này.

## Cập nhật 2026-09-29 — hoàn tất REST và nhãn phần trăm

- Nhãn “Ưu đãi” được thay bằng “Giảm X%” trên thẻ sản phẩm và chi tiết; thay đổi theo giá phiên bản đang chọn. Dùng giá thực tế từ API, không ghi cứng mức giảm.
- Toàn bộ controller MVC/JSP cũ, service chỉ phục vụ MVC và cấu hình VNPay/đăng nhập cũ được chuyển nguyên vẹn sang `legacy/spring-mvc`. Không còn trong source set, component scan hoặc JAR. Bỏ bộ lọc exclude controller cũ trong `ShopApplication` vì không cần nữa. Ảnh tĩnh có thể được database tham chiếu vẫn giữ nguyên.
- REST `/api/v1` thay thế đầy đủ các module trong ma trận bên dưới. `AdminApiController` và `AdminCatalogService` xử lý người dùng/thương hiệu/sản phẩm; file UserController cũ chỉ còn trong kho lưu trữ. `SpaController` là controller HTML duy nhất, chỉ trả Vue index.
- Bổ sung `GET /api/v1/admin/{resource}/{id}`, `POST` tạo mới trả `201 + Location`; Vue đọc chi tiết mới nhất trước sửa. Dữ liệu người dùng không chứa password/token. Giữ kiểm tra ADMIN ở server.
- Xác minh email đổi sang `POST /api/v1/auth/verify` JSON + CSRF. Link email vẫn tới trang Vue `/verify?code=...`; GET API không thay đổi trạng thái. GET IPN VNPay giữ theo giao thức provider, có chữ ký.
- Chuẩn hóa lỗi route/method/media type thành JSON `404/405/415`. Endpoint MVC dạng GET xóa/khóa không còn hoạt động.
- **DONE — Kiểm chứng:** backend `mvnw.cmd package` đạt 29 test, 1 migration test skip (không đổi schema); build Vue và đóng gói JAR đạt. Bộ browser trên JAR/MySQL đạt **8/8**, bao gồm nhãn phần trăm, ảnh/lưu/preview, lựa chọn phiên bản, giỏ/COD/hủy/hoàn kho, admin/CSV/phân quyền và desktop/mobile. Kiểm tra archive JAR có 13 REST controller, không có controller MVC/JSP cũ. API chi tiết 8 loại tài nguyên admin và JSON `404/405` đã kiểm tra trực tiếp trên MySQL.

Log: `data/rest-completion-tests.log`, `rest-completion-ui.log`, `rest-completion-package.log`, `rest-completion-e2e.log`, `rest-completion-server.log`. Bản cuối chạy tại `http://127.0.0.1:8080`; Vite REST tại `http://127.0.0.1:5173`. `git diff --check` đạt. Không commit/push hoặc thay schema trong lượt này.

Trong lúc hoàn tất, working tree nhận thêm bộ frontend TypeScript/Tailwind độc lập, thay `App.vue`, `style.css`, entry HTML và package scripts; một số trang mới còn mock. Đã giữ nguyên các tệp mới, tạo `RestApp.vue`, `main-rest.js`, `rest.css` và `vite.rest.config.js` để các trang nối API tiếp tục hoạt động. `dev`/`build` dùng entry REST; lệnh riêng `dev:typescript`/`build:typescript` giữ bộ scaffold ngoài bản triển khai. Khôi phục dependency/script Playwright bị mất trong lần thay package. Không ghi đè hoặc coi bộ scaffold là chức năng đã kiểm chứng.

Test ảnh đã sửa fixture: chọn sản phẩm đang có trong public catalog theo ID, không lấy dòng admin đầu tiên vốn có thể là sản phẩm thử nghiệm đã ngừng bán. Cơ chế 404 của sản phẩm ngừng bán vẫn giữ nguyên.

Không thay đổi schema hoặc xóa dữ liệu MySQL `techshop`. Kho MVC giữ bản nguồn để đối chiếu; không phải phần ứng dụng đang chạy. Các kết quả trước lần này ở dưới là lịch sử.

## Cập nhật 2026-09-29 — MySQL và ô chọn màu

- **DONE — Chuyển database:** MySQL80, schema `techshop`, `127.0.0.1:3306`; đã sao lưu schema rỗng và H2 ngoại tuyến trước khi chuyển. 21 bảng được chép trong transaction và so sánh toàn bộ giá trị/số dòng trước commit; giữ khóa ngoại bật. Đã chuyển 37 sản phẩm, 8 phiên bản, 40 ảnh, 2 tài khoản, 9 đơn/9 dòng đơn cùng các bảng liên quan. Tệp upload giữ nguyên. Không thay đổi schema khác trên MySQL.
- **DONE — Khởi động:** profile `dev,mysql-local` dùng Flyway V1–V5 và Hibernate validate. Tài khoản database riêng chỉ có quyền `techshop.*`, cấu hình ignored `data/mysql-local.properties`; tài khoản đăng nhập được giữ nguyên. Script `run-backend.ps1` mặc định MySQL; `-Packaged` mở JAR, `-Database h2` mở bản H2 cũ khi chủ động chọn.
- **DONE — Tùy chọn dạng ô:** chia theo thuộc tính, màu có thumbnail, lựa chọn có viền đỏ/dấu tích như mẫu, cấu hình không có sẵn bị khóa. Đổi màu loại bỏ cấu hình không tương thích; SKU/giá/kho/ảnh và cart vẫn theo đúng phiên bản. Hỗ trợ bàn phím qua radio thật.
- **DONE — Chữ:** tăng chữ nhỏ 8–10px lên 12px, 11–13px tăng thêm 2px trên desktop/mobile. Giữ cỡ tiêu đề lớn; kiểm tra không tràn ngang.
- **DONE — Sửa lỗi phát hiện trên MySQL:** form admin chỉ hiển thị khi ảnh/thông số đã chuyển xong sang dữ liệu chỉnh sửa, tránh lưu trong lúc tải gây lỗi `.split is not a function`. Test upload/preview/lưu/reload đã đạt sau sửa; ảnh của sản phẩm thử nghiệm được phục hồi.
- **DONE — Kiểm chứng bản JAR cuối:** `npm.cmd --prefix frontend run build` và `mvnw.cmd -DskipTests package` đạt. `$env:E2E_URL='http://127.0.0.1:8080'; npm.cmd --prefix frontend run test:e2e`: **8/8 đạt** trên MySQL techshop, gồm upload/preview/lưu/reload, catalog desktop/mobile, COD/fulfillment/review, admin/CSV/phân quyền, đổi ô màu/cấu hình hết hàng, tạo/mua/hủy biến thể và hoàn kho. Đăng nhập admin sau chuyển đã xác nhận. Sản phẩm có tên `e2e-variant-*` do kiểm thử tạo đã ngừng hiển thị, giữ nguyên lịch sử đơn.

Backup: `data/backups/mysql-switch-20260929-071250`. Log chuyển: `data/mysql-data-transfer.log`. Log build/browser/server: `data/mysql-ui-build.log`, `mysql-ui-package.log`, `mysql-ui-e2e-final.log`, `mysql-local-server.log`. Ảnh minh chứng: `frontend/test-results/options-desktop.png`, `options-mobile.png`, `variant-mobile.png`. Hướng dẫn: README và DATABASE.md. Không chạy các test Java có fixtures/create-drop vào schema `techshop`; E2E chỉ dùng tài khoản và dữ liệu demo. Tệp kết nối root tạm đã được gỡ, chỉ giữ tài khoản ứng dụng có quyền trên schema này.

Các kết quả 28/09 và 18/09 phía dưới là lịch sử trước lần chuyển này. Blocker dịch vụ ngoài (SMTP thật, VNPay sandbox keys, Docker Engine) giữ nguyên; không cản trở MySQL native và mua hàng COD.

## Rà soát lại 2026-09-28

### Tiếp tục triển khai 28/09 — DONE: ảnh và tùy chọn/biến thể

Đã thêm ProductVariant và thuộc tính tùy ý, migration V5, CRUD admin, lựa chọn phiên bản trên trang chi tiết, cart theo (product, variant), snapshot SKU/tùy chọn/giá, trừ/hoàn kho cho COD và VNPay. Bổ sung thứ tự ảnh lưu trong database. Frontend build đạt; H2 27 test đạt, MySQL 26 test API đạt và 1 test migration V1 → V5 đạt. Cả 7 luồng browser đạt trên JAR cuối. JAR đang phục vụ http://127.0.0.1:8080, Vite ở port 5173; database demo giữ nguyên. MySQL kiểm thử đã dừng sau khi hoàn tất.

| Kiểm tra ngày 28/09 | Kết quả |
|---|---|
| `npm.cmd --prefix frontend run build` | PASS |
| `.\mvnw.cmd clean package` | PASS: 27 đạt, 1 migration test skip (chạy riêng trên MySQL) |
| `.\mvnw.cmd -DskipTests package` | PASS: đóng gói lại sau sửa hai chuỗi tiếng Việt |
| `scripts/test-mysql.ps1 -Instance mysql-variants-test` (PowerShell Bypass) | PASS: 26/26, Flyway V1–V5 và Hibernate validate |
| `scripts/test-legacy-migration.ps1 -Instance mysql-variants-test` | PASS: baseline V1 → V5; giữ sản phẩm/đơn/voucher cũ, variant nullable, chạy lại không đổi |
| Browser trên Vite | 6 luồng cũ đạt; luồng mới tạo/mua/hủy phiên bản đạt sau sửa nhãn form |
| `$env:E2E_URL='http://127.0.0.1:8080'; npm.cmd --prefix frontend run test:e2e` | PASS 7/7: ảnh upload/preview/lưu/reload, catalog desktop/mobile, COD/fulfillment/reviews, routes/CSV/admin, quyền truy cập, tạo/mua/hủy biến thể và hoàn kho |
| `java -jar target/techshop.jar --spring.profiles.active=dev --server.address=127.0.0.1` | PASS: HTTP/API 8080, database H2 hiện có |

Test mới kiểm tra tách giỏ theo phiên bản, giá server, thiếu/sai phiên bản, hết hàng, ngừng bán, snapshot không đổi theo sửa thuộc tính, hai người mua chiếc cuối, hủy lặp và callback thanh toán thất bại hoàn kho đúng phiên bản. Admin API kiểm tra tạo/sửa/deactivate và tổ hợp trùng. Demo thêm một sản phẩm bốn phiên bản (35 sản phẩm seed).

MySQL instance cũ gặp lỗi truy cập tệp redo trên Windows; giữ nguyên thư mục đó, dùng instance cô lập `data/mysql-variants-test` cùng port 3307. Không truy cập database nghiệp vụ. Logs mới: `data/variants-package.log`, `variants-mysql-tests.log`, `variants-legacy-migration.log`, `variants-jar-e2e.log`, `variants-jar-server.log`.

### Lịch sử khảo sát trước triển khai (các TODO dưới đây đã được xử lý)

- Đã đọc mã nguồn, git status, migration và báo cáo test trên đĩa; lượt rà soát này không chạy lại build/test hoặc khởi động server.
- Yêu cầu mới về tùy chọn màu sắc/phiên bản: **TODO**. Chưa có entity/bảng variant, admin chưa có form tùy chọn, ProductPage chưa có bộ chọn, AddItem chỉ nhận productId/quantity; cart/order chưa lưu variant. Migration hiện dừng ở V4.
- Ảnh: **DONE về code và test lần trước** (preview, mở ảnh, ảnh đại diện, bỏ ảnh, thumbnail, upload và lưu URL vào database). Log xác nhận JAR đã build ngày 18/09 lúc 11:39; lần khởi động lại sau đó bị công cụ chặn do hết hạn mức. Không coi dòng “JAR hiện chạy” ở ghi chép cũ là trạng thái server hiện tại.
- Kết quả test bên dưới là lịch sử 18/09, không phải test mới ngày 28/09. Test ảnh chạy riêng đạt; chưa có bằng chứng cả 6 browser tests đã chạy chung sau bổ sung ảnh.
- Dev dùng H2 file data/techshop-demo.mv.db; MySQL có cấu hình/migration và test riêng. Sản phẩm không phải mock chỉ nằm ở frontend.

Cập nhật 2026-09-18. Khảo sát working tree đang có nhiều thay đổi chưa commit, giữ phần hoạt động và tiếp tục triển khai. Không reset/revert/commit/push. Không kết nối database nghiệp vụ cũ.

DONE = có và kiểm tra cục bộ; PARTIAL = còn phần chưa kiểm chứng/loại bỏ; TODO = chưa triển khai; BLOCKED = cần môi trường/dịch vụ ngoài. Các trạng thái không hàm ý đã triển khai production.

| Chức năng cũ | Backend API (/api/v1) | Vue page/component | Trạng thái | Test |
|---|---|---|---|---|
| HomePageController: trang chủ/banner/danh mục | products, categories, brands, banners | TechHome | DONE | Browser desktop/mobile, API thật |
| Danh sách, chi tiết sản phẩm | products, products/{id} | CatalogPage, ProductPage | DONE | AdminApiTests; browser search, chi tiết |
| AuthController: đăng ký/login/logout/xác minh/recovery | auth/* | AuthPage | DONE | CommerceApiTests; browser USER/ADMIN |
| ClientController: hồ sơ, mật khẩu | account, account/password | AccountPage | DONE | Hồ sơ riêng từng user, mật khẩu cũ/mới, logout; route refresh |
| CartController: thêm/sửa/xóa/chọn dòng | cart/items, cart/quote | CartPage | DONE | API chọn một phần giỏ, browser đổi số lượng |
| OrderController: COD/history/tracking/cancel | orders, orders/{id}/status | CheckoutPage, OrdersPage | DONE | Pricing, snapshot, ownership, retry đồng thời, oversell, hủy/hoàn kho |
| VoucherController | admin/vouchers, cart/quote, orders | AdminPage, CheckoutPage | DONE | Invalid/inactive/future/expired/minimum/cap/limit và tranh chấp lượt trên H2/MySQL |
| PromotionController | admin/promotions; giá sản phẩm | AdminPage, ProductCard | DONE | API admin, browser route, demo có promotion |
| ReviewController: tạo/sửa/xóa của khách | products/{id}/reviews[/{reviewId}] | ProductPage | DONE | Đã mua/nhận, ownership sửa/xóa; browser tạo/sửa/xóa |
| AdminReviewController | admin/reviews | AdminPage | DONE | API list/filter; browser route |
| ProductController: CRUD/ảnh/thông số/bật tắt | admin/products, admin/uploads | AdminPage | DONE | CRUD/quan hệ/deactivate; MIME, extension, chữ ký ảnh |
| CategoryController / AdminBrandController | admin/categories, admin/brands | AdminPage | DONE | CRUD fixtures, ràng buộc đang sử dụng, browser routes |
| AdminBannerController | admin/banners | AdminPage, TechHome | DONE | API/browser, banner demo công nghệ |
| PolicyController / ClientPolicyController | admin/policies, policies | AdminPage, PolicyPage | DONE | Browser sửa policy, trang khách/refresh, search backend |
| UserController: list/quyền/khóa/xóa | admin/users | AdminPage | DONE | USER bị chặn admin; list/filter không lộ secret, khóa vô hiệu phiên |
| Admin OrderController | admin/orders, status, restore | AdminPage | DONE | State transition, archive giữ lịch sử, browser fulfillment |
| DashboardController | admin/dashboard | AdminPage /admin | DONE | Database statistics/date filter; desktop/mobile |
| InvoiceController: PDF | admin/orders/{id}/invoice | AdminPage | DONE | API PDF; font classpath |
| Báo cáo đối soát bổ sung | admin/reports/orders | AdminPage | DONE | API CSV, browser download; không có module Excel legacy riêng |
| VNPay | orders/{id}/payment, payments/vnpay/ipn | CheckoutPage, PaymentReturnPage | DONE / BLOCKED | 3 callback tests cục bộ đạt; giao dịch ngoài cần sandbox key |
| JSP/MVC cũ | Chuyển nguồn cũ sang legacy/spring-mvc; REST /api/v1 thay thế | Vue thay thế các route chính | DONE (29/09) | Kiểm thử handler REST; JAR không chứa controller MVC/JSP cũ |

## Completed

- Bổ sung theo phản hồi upload: admin có gallery xem trước, mở ảnh đầy đủ, bỏ ảnh, chọn ảnh đại diện và thumbnail trong danh sách sản phẩm. Thông báo phân biệt tải tệp với bấm Lưu thay đổi để lưu liên kết vào database. Playwright `uploaded product image previews and persists after save and reload` đã đạt trên Vite: xác nhận tệp thật trong data/uploads, ảnh giải mã được, lưu/refresh admin và API/trang chi tiết khách trả ảnh đã lưu. Build frontend đạt; đang cập nhật JAR phục vụ port 8080.

- Giữ Spring Boot, Java target 17, JPA/MySQL, REST /api/v1; Vue 3/Vite/Pinia/Router và API client chung. Không nâng dependency hàng loạt.
- Session HttpOnly, CSRF, CORS theo CLIENT_URL; backend kiểm tra role/trạng thái tài khoản và ownership. JSON lỗi 401/403 thay redirect JSP.
- Checkout đọc lại giá/kho, tính promotion/voucher/shipping, snapshot tên/SKU/giá; khóa người mua/sản phẩm, refresh locking read phù hợp MySQL REPEATABLE READ. Retry cùng key/payload trả cùng đơn, không trừ kho hai lần; payload khác bị từ chối.
- Thêm ghi chú giao hàng tối đa 1.000 ký tự, lưu/hiển thị ở khách và admin.
- Voucher có active, ngày hiệu lực, minimumOrder, maximumDiscount, usageLimit/usedCount. Khóa voucher chống vượt lượt khi đặt đồng thời. Chỉ tăng lượt khi commit thành công; retry không tăng lại. Hủy/thanh toán thất bại/hết hạn thanh toán hoàn lại một lượt (từ 2026-09-30). Giữ model giảm phần trăm, không tự thêm fixed discount.
- Hoàn thiện sửa/xóa đánh giá của chính khách. Public DTO không lộ email. Sửa lỗi form mất nội dung trong lúc sản phẩm liên quan đang tải.
- Upload kiểm tra chữ ký, MIME, extension, kích thước, tên/đường dẫn an toàn. Lỗi query/upload/tranh chấp/server có JSON tiếng Việt, không trả stack trace.
- Sửa tràn ngang dashboard mobile. Trang chủ có danh sách nổi bật/mới từ API.
- Seed dev idempotent: 34 SKU, 8 danh mục, 2 thương hiệu, hàng hết/còn, promotion, banner, voucher/policy. Restart giữ số sản phẩm và mật khẩu.
- Migration V4 additive; default cho schema update H2 cũ (kể cả adminArchived của V3). MySQL dùng Flyway + validate.
- Xóa HomeView thực phẩm và các Vue auth/policy cũ không còn được import/router dùng sau khi kiểm chứng trang thay thế; xóa HelloWorld/logo Vite/Vue. Giữ JSP/MVC/resources cũ chưa xác minh hết tham chiếu lịch sử.
- README, frontend README, API.md, DATABASE.md và env mẫu đã đối chiếu cấu hình thực tế.

## Remaining

Ảnh và tùy chọn/biến thể đã triển khai xuyên suốt database, admin, trang chi tiết, cart, checkout, PDF và snapshot đơn hàng. SMTP/VNPay ngoài cần cấu hình, Docker cần Engine hoạt động. MVC/JSP đã chuyển ra khỏi nguồn ứng dụng vào legacy/spring-mvc; ảnh tĩnh vẫn giữ để không mất URL trong dữ liệu.

## Known issues

- Không có lỗi chức năng tái hiện còn mở trong bộ test đã chạy; kiểm thử hữu hạn không bảo đảm không còn lỗi.
- Chưa test tải lớn/nhiều instance; session nằm trong bộ nhớ ứng dụng.
- Warning có sẵn: DaoAuthenticationProvider deprecated, PageImpl serialization, unchecked code legacy; giữ tương thích API hiện có.
- Model hiện có variants và thuộc tính tùy ý; sản phẩm cũ vẫn mua bình thường. Promotion demo hết hạn một năm từ seed đầu, không tự gia hạn khi restart.

## Tests executed — lịch sử 18/09 (kết quả 28/09 ở đầu tài liệu)

Windows, Java 21.0.8 biên dịch target 17, Chrome headless, H2 2.3.232, MySQL 8.0.44 riêng port 3307. Chưa chạy runtime Java 17 trên máy này; CI/Docker cấu hình Java 17.

| Command | Kết quả |
|---|---|
| git status / git diff / git diff --check | Khảo sát, giữ thay đổi chưa commit; diff check đạt |
| npm run build | PowerShell chặn npm.ps1; dùng npm.cmd, không đổi policy toàn máy |
| npm.cmd run build (frontend) | PASS |
| .\mvnw.cmd test | Ban đầu sandbox chặn Maven Central; chạy lại với quyền mạng. Lượt gần nhất: 20 pass, 1 migration test skip |
| .\mvnw.cmd clean package | PASS: 22 pass, 1 migration test skip; target/techshop.jar chứa Vue |
| powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-mysql.ps1 | PASS: 21 tests, Flyway V1–V4 + Hibernate validate, gồm oversell/retry/voucher đồng thời |
| powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-legacy-migration.ps1 | PASS: 1 test, V1 → V4 giữ sản phẩm/đơn/voucher cũ, migrate lại không thay đổi |
| powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/run-backend.ps1 | Đã khởi động dev sau khi sửa default schema H2 cũ |
| npm.cmd run dev -- --host 127.0.0.1 | PASS, frontend 5173/backend 8080 |
| npm.cmd run test:e2e | Phát hiện và sửa schema/mobile/form review. Các test routes và luồng COD mở rộng đã pass |
| $env:E2E_URL='http://127.0.0.1:8080'; npm.cmd run test:e2e | PASS: 5/5, desktop/mobile, COD/fulfillment/notes/reviews, admin routes/CSV, USER forbidden, route refresh, không có pageerror/server 5xx trong test giám sát |
| java -jar target/techshop.jar --spring.profiles.active=dev --server.address=127.0.0.1 | PASS khởi động, Vue/API cùng origin, giữ H2 demo hiện có |
| docker info --format '{{.ServerVersion}}' | Docker Engine pipe không tồn tại; chưa chạy Docker build/compose |

Không có script lint/typecheck; frontend JavaScript. Không bỏ security/validation để làm test pass. API tests cô lập; browser tests tạo đơn/chuyển trạng thái trên demo. Không gửi mail/thanh toán thật.

Logs ignored: data/current-package.log, current-h2-tests.log, current-mysql-tests.log, current-legacy-migration.log, current-jar-server.log. Browser report/ảnh/trace: frontend/playwright-report và frontend/test-results.

## Blockers

- SMTP ngoài chưa cấu hình; dev mail ghi .eml ở data/dev-mail, auth tests mock mail.
- Chưa có VNPay sandbox merchant/secret để test giao dịch bên ngoài. COD độc lập; gateway tắt mặc định.
- Docker Engine không chạy; không chặn native MySQL/JAR.

## Next action

Khi triển khai ngoài máy phát triển: cấp SMTP/VNPay sandbox nếu sử dụng, bật Docker Engine nếu chạy Compose. Khi chuyển database thật, sao lưu và thử DATABASE.md trên bản sao trước; không dùng create-drop hoặc seed demo trên dữ liệu thật. Port native: backend/JAR 8080, Vite 5173.
