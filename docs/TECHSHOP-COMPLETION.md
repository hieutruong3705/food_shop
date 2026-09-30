# Hoàn thiện TechShop — 29/09/2026

**Cập nhật tiếp 30/09:** bổ sung gán nhiều hãng cho danh mục, validation sản phẩm, bốn phạm vi chung cho khuyến mãi/voucher với bộ chọn tìm kiếm và chip, bảo hành lọc bằng thanh kéo. Có Flyway V6 giữ dữ liệu cũ. File và cách test tại [CATEGORY-BRAND-SCOPES.md](CATEGORY-BRAND-SCOPES.md).

**Cập nhật 30/09:** mega menu rút còn 3 phần, bỏ nút pause/play, tổng hợp/quyền đánh giá, thao tác nhanh đơn và layout admin riêng. Xem [ADMIN-REVIEW-REFINEMENT.md](ADMIN-REVIEW-REFINEMENT.md) cho hành vi mới nhất, file thay đổi và cách test. Các mô tả pause/play và menu đầy đủ dưới đây ghi lại đợt triển khai trước.

## Kết quả kiểm chứng

- Bổ sung luân phiên voucher/khuyến mãi: 4/4 ExperienceApiTests đạt, bài browser banner/offer/chat đạt trên MySQL, build/package thành công. Log mới: `data/offers-tests.log`, `offers-e2e.log`, `offers-build.log`, `offers-package.log`. Kiểm tra có hơn 6 mã, chuyển tự động/thủ công, dừng khi thao tác, mức giảm/điều kiện theo mã và khuyến mãi cho sản phẩm đã ngừng bán.

- Frontend build và JAR package cuối thành công; ứng dụng chạy tại http://127.0.0.1:8080 trên MySQL techshop.
- Backend: 35 test đạt và 1 test migration skip theo cấu hình, tính gộp bộ kiểm thử hiện có và 3 test Experience mới. Xem `data/experience-tests.log`, `experience-new-tests.log`, `experience-package-final.log`.
- Browser: lượt đầy đủ 12/12 đạt (`data/experience-e2e.log`). Sau các chỉnh sửa cuối, kiểm tra danh mục/query đạt; lỗi lớp template khiến nút banner bị ẩn đã được sửa và kiểm tra lại auto/manual, voucher, chat desktop/mobile đạt (`data/experience-carousel-retest.log`).
- Hai lượt dọn đã ẩn tổng cộng 27 fixture sản phẩm có tên/SKU khớp chính xác mẫu test, có backup trong `data/backups`; lần xem trước cuối không còn fixture khớp. Banner/voucher của ca test bị gián đoạn cũng được sao lưu và dọn, không đụng dữ liệu kinh doanh khác.
- Không kiểm thử giao dịch VNPay hoặc gửi email qua nhà cung cấp thật trong đợt này. Chat ban đầu là mock; đợt bổ sung 30/09 chuyển sang Gemini khi có khóa backend, xem [CHATBOT.md](CHATBOT.md).

## Đã triển khai

### Menu danh mục

- Hãng có sản phẩm đang bán thuộc danh mục; icon/logo lấy từ DB.
- Top 10 lấy tại `OrderDetailRepository` bằng `SUM(order_details.quantity)`, nhóm product_id, sắp giảm dần, giới hạn 10 qua Pageable. Tên bảng thực tế là `order_details`, không phải `order_items`.
- Đơn hợp lệ trong bảng xếp hạng: CONFIRMED, SHIPPING, COMPLETED; COD hoặc thanh toán PAID. Không tính PENDING, CANCELLED, đơn online chưa thanh toán. Chỉ xếp sản phẩm đang bán trong danh mục đó.
- Chưa có doanh số: hiển thị nhãn “Sản phẩm mới”, lấy sản phẩm mới nhất, ưu tiên còn tồn. Không dựng số lượng bán giả.
- Hai thanh kéo giá lấy min/max DB; hãng/giá/thông số giữ query hiện tại khi cùng danh mục, xóa page khi đổi bộ lọc. Sang danh mục khác không mang thông số của danh mục cũ.

### Banner và ưu đãi

- Banner lớn dùng các banner active thuộc catalog công nghệ trong DB; sắp ID giảm dần. Entity hiện không có displayOrder/startDate/endDate, nên không đoán hoặc thêm các field này.
- Tự chuyển mỗi 4.500 ms; trái/phải, dot, nút tạm dừng; reset khi thao tác. Tạm ngừng khi rê chuột, focus, tab ẩn hoặc hệ điều hành yêu cầu giảm chuyển động. Một banner thì hai nút bị vô hiệu hóa; thêm banner active thứ hai để thấy tự chuyển.
- Không có banner: hình minh họa mặc định dẫn về danh mục thực tế. Khi DB đã có banner thì không trộn banner giả.
- Hai ô phải lấy khuyến mãi và voucher còn hiệu lực từ API, luân phiên mỗi 4,5 giây; có nút trước/sau, số thứ tự, tạm dừng. Hover/focus dừng chuyển để đọc và sao chép; thao tác đặt lại timer. Không giới hạn còn 6 mã nên mọi mã đủ điều kiện đều được hiển thị.
- Khuyến mãi hiện rõ “Giảm X%”. Chương trình còn hạn, có cấu hình sản phẩm/hãng nhưng tất cả sản phẩm đã ngừng bán vẫn hiện phần trăm kèm thông báo chưa mở bán; không tạo link đến danh sách trống. `hasAvailableProducts` cho biết có sản phẩm đang kinh doanh. Không tự bật lại sản phẩm hoặc thay mức giảm trong DB.
- Voucher xét active, hạn, usage limit; hiển thị mã, phần trăm, min order, giới hạn giảm và danh mục. Có sao chép mã. Điều kiện giỏ hàng vẫn do backend kiểm tra lại khi đặt hàng. Không có ưu đãi thì hiện thông báo trống, không tự tạo mã.

### TechShop AI

- Nút nổi, cửa sổ TechShop AI/Online, thu nhỏ/đóng, tin bot trái/user phải, gợi ý, Enter/Gửi, loading ba chấm, tự cuộn; giữ hội thoại trong phiên trang khi thu nhỏ.
- `GeminiChatService` gọi Google từ backend khi cấu hình khóa; không có khóa thì giữ mock trong `ChatApiController`. Frontend chỉ gọi backend; có lịch sử 8 tin, trạng thái provider, xử lý lỗi/quota. Cấu hình local không nằm trong Git.
- Tin nhắn tối đa 1.000 ký tự, không rỗng; CSRF giữ nguyên. Nội dung được Vue escape, không render HTML từ phản hồi.

## Lỗi logic đã sửa trong đợt công việc

1. Sản phẩm ngừng bán vẫn hiện nút “Ngừng bán”: thay bằng “Bán lại”, API PATCH chỉ cập nhật active, giữ stock/giá/SKU/trạng thái phiên bản. Gửi lại cùng trạng thái không đảo trạng thái.
2. Hết hàng chen trước hàng còn tồn: SQL ưu tiên còn hàng trước khi phân trang, sau đó mới áp dụng kiểu sort. Dùng cho home/catalog/search/related và admin. Bảng top 10 riêng giữ thứ tự doanh số đúng yêu cầu.
3. Ngừng bán chưa khóa cùng bản ghi như checkout: thao tác trạng thái và DELETE soft-delete nay khóa parent product.
4. Giỏ hàng cho tiếp tục với dòng đã ngừng/hết hàng/vượt tồn: không tự chọn, không cho chọn dòng không hợp lệ; thiếu dòng hợp lệ thì không có nút đặt hàng. Vẫn cho giảm số lượng về mức hợp lệ hoặc xóa dòng. Backend tiếp tục kiểm tra lại trước trừ tồn.
5. Cart active bỏ sót technologyCatalog: đồng bộ trạng thái không kinh doanh; bỏ link chi tiết khi dòng không còn bán. Cập nhật số lượng thất bại trả input về số lượng thực tế.
6. Bộ lọc admin trống nhãn và trang cuối rỗng sau đổi trạng thái: khởi tạo giá trị mặc định và quay về trang hợp lệ.
7. Banner trỏ tên demo cố định gây trang trống khi ngừng bán: thay bằng banner DB hoặc danh mục thực tế.
8. Mega menu làm mất bộ lọc cùng danh mục: giữ query và reset trang.
9. Ưu đãi công khai phải khớp điều kiện thực tính giá: không hiện promotion thiếu lịch hoặc hết hạn, voucher hết lượt/hết hạn.
10. Test phụ thuộc sản phẩm demo do người dùng bật/tắt: tạo fixture riêng, kiểm tra badge nếu thực sự có giảm giá, không bật lại sản phẩm của người dùng để làm test đạt.

## Rà soát các phần đã có

- Giá/giảm giá/phí vận chuyển/tổng đơn tính server-side; request checkout không nhận giá frontend.
- Quantity API có Min(1)/Max(100), kiểm tra stock và phiên bản thuộc đúng sản phẩm; lock và test concurrent checkout chống bán quá tồn.
- Đơn: PENDING → CONFIRMED/CANCELLED; CONFIRMED → SHIPPING/CANCELLED; SHIPPING → COMPLETED. User chỉ hủy PENDING. Online chưa PAID không xác nhận/giao; hoàn tồn đúng một lần.
- Voucher: active, lịch, min order, category, maximum discount, usage limit; khóa voucher chống vượt lượt đồng thời.
- Auth/profile: hash mật khẩu, xác minh/reset token, chống đổi quyền qua profile, login đổi session ID, redirect nội bộ. Router chặn trang admin; backend chặn cả API. AccountStatusFilter kiểm tra khóa/quyền mới trên mỗi request.
- CRUD sản phẩm/danh mục/hãng/banner/voucher/khuyến mãi/đơn/user: giữ validation, CSRF, quan hệ và soft-delete sản phẩm; không xóa hãng/danh mục còn tham chiếu; không tự khóa/hạ quyền tài khoản admin đang dùng.
- Các kết luận dựa trên đọc code và kiểm thử phạm vi này; không phải chứng nhận không còn mọi lỗi có thể xảy ra. Dịch vụ SMTP/VNPay bên ngoài cần cấu hình riêng nếu sử dụng.

## Endpoint, query và schema

| Endpoint/query | Nội dung |
|---|---|
| GET /api/v1/catalog/menu?categoryId=... | Facets + bestSellers + rankingSource |
| GET /api/v1/offers | promotions và vouchers đủ điều kiện công khai |
| GET /api/v1/products?promotionId=... | Lọc sản phẩm/hãng thuộc promotion đang hiệu lực, kết hợp bộ lọc hiện có |
| PATCH /api/v1/admin/products/{id}/status | Body `{"active":true}` hoặc false; ADMIN + CSRF |
| POST /api/chat | `{"message":"..."}` → `{"reply":"..."}`; public + CSRF |
| POST /api/v1/chat | Alias chuẩn prefix REST cho cùng chat handler |

Không có migration mới: dùng đúng các field hiện hữu. Giữ Flyway và `ddl-auto=validate`; không đổi sang update.

## Dữ liệu kiểm thử

`scripts/hide-test-fixtures.mjs` chỉ nhận bản ghi có name=sku, khớp chính xác mẫu fixture trong các bài kiểm thử: facet-13 chữ số-0/1, selling-13 chữ số, option-fixture-13 chữ số, e2e-variant-13 chữ số. Chế độ mặc định chỉ xem trước; `--apply` sao lưu vào `data/backups/` trước khi đặt active=false, technologyCatalog=false. Không xóa đơn hoặc lịch sử, không tự xóa toàn bộ dữ liệu có chữ “demo”. Bản ghi vẫn có thể kiểm tra trong admin.

## File sửa/tạo

- Backend: `api/controller/CatalogApiController.java`, `AdminApiController.java`, **`OffersApiController.java`**, **`ChatApiController.java`**; `api/service/AdminCatalogService.java`, `CommerceService.java`; `repository/OrderDetailRepository.java`; `config/ApiSecurityConfig.java` (đều dưới `src/main/java/com/example/shop/`).
- Frontend: `src/RestApp.vue`, `main-rest.js`, `services/api.js`, `components/CategoryMenu.vue`, **`components/HomeHero.vue`**, **`components/OfferCarousel.vue`**, **`components/TechChat.vue`**, `views/TechHome.vue`, `CatalogPage.vue`, `CartPage.vue`, `AdminPage.vue`, **`experience.css`** (dưới `frontend/`).
- Backend tests: `AdminApiTests.java`, `CommerceApiTests.java`, `RestArchitectureTests.java`, **`ExperienceApiTests.java`**.
- Browser tests: `catalog.spec.js`, `retail.spec.js`, `shop.spec.js`, **`selling.spec.js`**, **`experience.spec.js`**.
- Script: **`scripts/hide-test-fixtures.mjs`**. Tài liệu: `docs/API.md`, `CATALOG-UI.md`, `MIGRATION.md`, **`TECHSHOP-COMPLETION.md`**.

## Test nhanh

1. Admin → Sản phẩm → lọc Ngừng kinh doanh → Bán lại. Stock không đổi; sản phẩm rời bộ lọc ngừng bán. Mở catalog theo giá/bán chạy/mới nhất và sang trang: hết hàng ở cuối.
2. Thêm vào giỏ rồi ngừng sản phẩm hoặc đặt stock=0 trong admin; tải lại giỏ: không chọn/đặt dòng đó được. Bán lại và thêm stock thì mua được.
3. Mở menu một danh mục: đúng hãng, top bán chạy hoặc nhãn fallback, kéo giá và áp dụng. Chọn hãng/thông số khi URL có inStock/minPrice: query còn đúng.
4. Admin → Banner: thêm hai ảnh, bật Hiển thị và catalog công nghệ. Trang chủ: chờ 4,5 giây, thử hai mũi tên, dot, pause; hover/focus sẽ dừng tự chuyển.
5. Admin → Khuyến mãi/Voucher: thử active, tương lai, hết hạn, hết lượt; ô bên phải chỉ dùng dữ liệu đủ điều kiện. Bấm promotion ra danh sách áp dụng, mã voucher sao chép được và server kiểm tra lại khi checkout.
6. Bấm TechShop AI: gửi bằng Enter, thử bốn gợi ý, thu nhỏ/mở lại; xem Network gọi `/api/chat`, không có request AI bên ngoài.
7. Đăng nhập USER: `/admin` bị chặn; request `/api/v1/admin/...` trả 403. Không đăng nhập trả 401; request thay đổi thiếu CSRF trả 403.

Lệnh: `npm.cmd --prefix frontend run build`; `.\mvnw.cmd test`; `$env:E2E_URL='http://127.0.0.1:8080'; npm.cmd --prefix frontend run test:e2e`. Java tests dùng profile test cô lập, không chạy create-drop vào techshop.
