# Hoàn thiện menu, banner, đánh giá và quản trị — 30/09/2026

## Kết quả kiểm chứng

- Build Vue và package JAR thành công. Bản cuối chạy MySQL techshop tại http://127.0.0.1:8080.
- Bộ backend: 40 ca đạt, 1 migration test skip theo cấu hình (`data/admin-refine-tests.log`). Sau khi thêm assertion review summary/duplicate/invalid rating/order actions, chạy lại 17 CommerceApiTests đều đạt (`data/admin-refine-package.log`).
- Browser: **13/13 đạt** trên JAR/MySQL (`data/admin-refine-e2e.log`), gồm menu/filter, banner/voucher, Gemini UI, giỏ, COD, xử lý đơn ngay danh sách, đánh giá, CRUD, ảnh, admin desktop/mobile và chặn user thường.
- `git diff --check` đạt. Fixture sản phẩm của lượt kiểm thử đã ẩn với backup `data/backups/test-fixtures-1790703003859.json`. Không xóa lịch sử đơn hàng.

## Logic thay đổi

- Mega menu chỉ có sản phẩm bán chạy/mới, hãng và giá. Giữ query bán chạy DB và fallback hiện có; hãng/min/max lấy từ sản phẩm trong danh mục. Bộ lọc thông số đầy đủ vẫn ở trang catalog, không nằm trong menu.
- Không tạo bảng category-brand dư thừa: Product đã có cả category và brand. Admin quản lý quan hệ qua form sản phẩm; trang danh mục/thương hiệu có hướng dẫn và link đến sản phẩm. Cửa hàng chỉ hiện hãng có sản phẩm đang mở bán trong danh mục.
- Banner/promotion/voucher tự đổi mỗi 4,5 giây, reset timer khi bấm mũi tên. Bỏ các nút pause/play. Hover tạm ngừng để đọc; voucher ngừng khi focus vào nội dung để sao chép/đọc điều kiện; tab ẩn và reduced-motion không tự chạy. Focus nút mũi tên không giữ timer dừng vô thời hạn.
- Review đã có kiểm tra mua hàng COMPLETED, rating 1–5, nội dung không trắng tối đa 2.000 ký tự, mỗi user/product một review. Khóa user ở backend trước kiểm tra và ghi để ngăn hai request đồng thời tạo trùng; giữ sửa/xóa review của chính mình. Không cần đổi schema.
- Thêm tổng hợp count/AVG ở repository, hiển thị trên trang chi tiết; API trả canReview, chỉ hiện form khi đã nhận hàng và chưa review. Sau thêm/sửa/xóa, tải lại cả danh sách và tổng hợp.
- Danh sách admin order có Xác nhận/Chuyển sang đang giao/Hoàn thành/Hủy tùy allowedStatuses của backend. Backend và DTO dùng chung OrderTransitions, gồm điều kiện thanh toán: VNPay chưa PAID không được xử lý tiếp; đơn PAID không được hủy khi chưa có hoàn tiền. Không cho bỏ bước/quay lùi. Hủy có xác nhận và hoàn kho theo logic hiện có.
- Đổi trạng thái tải lại danh sách với đúng search/status/archived/page; xử lý trang cuối rỗng. Có thông báo kết quả, khóa nút khi đang gửi. Khi đang mở chi tiết thì chi tiết vẫn giữ và được cập nhật.
- AdminLayout riêng: sidebar/topbar/content cho toàn bộ route admin. Không có header, navbar, footer hoặc chat cửa hàng trong admin. Giữ guard ADMIN ở frontend và backend, CSRF ở API.

## Endpoint / schema

- Mới: `GET /api/v1/products/{id}/reviews/summary` → average, count, purchased, canReview. Public được xem count/average; eligibility theo người đăng nhập.
- OrderView thêm `allowedStatuses` (các thao tác dành cho quản trị); PATCH `/api/v1/admin/orders/{id}/status` giữ nguyên. Không cấp thêm quyền cho người mua qua field này; endpoint vẫn kiểm tra ADMIN.
- Không migration mới vì không đổi schema. Giữ Flyway và ddl-auto=validate.

## File sửa/tạo trong đợt này

- Mới: `frontend/src/components/AdminLayout.vue`, `frontend/src/admin-layout.css`, `src/main/java/com/example/shop/domain/OrderTransitions.java`, tài liệu này.
- Frontend: `RestApp.vue`, `main-rest.js`, `components/CategoryMenu.vue`, `HomeHero.vue`, `OfferCarousel.vue`, `views/AdminPage.vue`, `ProductPage.vue`.
- Backend (dưới `src/main/java/com/example/shop`): `api/service/CommerceService.java`, `api/dto/CommerceResponse.java`, `api/controller/ReviewApiController.java`, `repository/ReviewRepository.java`.
- Test: `src/test/java/com/example/shop/CommerceApiTests.java`, `frontend/tests/catalog.spec.js`, `shop.spec.js`, `experience.spec.js`.

## Test nhanh

1. Header cửa hàng → Danh mục → đổi danh mục: chỉ 3 cột; hãng đúng category, kéo min/max rồi Áp dụng giá; URL giữ category và các bộ lọc trước đó.
2. Admin → Sản phẩm → đổi category/brand của một sản phẩm đang bán; mở lại menu để thấy hãng được suy ra. Không phải gắn lại trong category.
3. Trang chủ: chờ 4,5 giây với con trỏ ngoài banner; bấm mũi tên rồi rời chuột, banner tiếp tục tự đổi. Hai ô ưu đãi cũng luân phiên nếu có nhiều dữ liệu; không có nút pause/play.
4. User chưa mua: thấy điểm và số đánh giá, không có form. Với đơn COMPLETED: gửi 1–5 sao, sửa/xóa; số lượng và trung bình cập nhật. Gọi API với rating 0/6, nội dung trắng, review trùng hoặc user khác đều bị chặn.
5. Admin → Đơn hàng → chọn bộ lọc Chờ xác nhận → Xác nhận ngay trên hàng: hàng rời bộ lọc, thông báo hiện; không đổi sang danh sách toàn bộ. Đơn đang giao chỉ có Hoàn thành, đơn kết thúc không có thao tác trạng thái.
6. Admin có layout riêng trên desktop/mobile; bấm Xem cửa hàng quay về layout khách. User thường vào /admin bị từ chối và API admin trả 403.
