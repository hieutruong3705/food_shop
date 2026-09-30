# Trạng thái công việc

Cập nhật 2026-09-30 (cuối ngày). Nhánh `appmod/java-upgrade-20260917082931`. Đã commit thành ba nhóm (backend, frontend, tài liệu). Chưa push.

## 1. Kiểm thử lúc chốt

| Hạng mục | Kết quả |
| --- | --- |
| `.\mvnw.cmd test` (H2) | 117 chạy, 0 lỗi, 1 bỏ qua (`LegacyMigrationTest`, cần MySQL cô lập) |
| MySQL 8 cô lập (Flyway V1–V11, `ddl-auto=validate`): `LegacyMigrationTest` và 7 lớp test chính | 82 đạt, 0 lỗi |
| `npm.cmd --prefix frontend run test:unit` (vitest) | 14 đạt |
| `npm.cmd --prefix frontend run build` | Đạt |
| E2E Playwright | 19/19 (`npx playwright test --reporter=list --output=../data/pw-output`) |
| Duyệt 19 trang × desktop/mobile bằng trình duyệt | Không tràn ngang, không ảnh hỏng, không lỗi console, không lỗi lazy loading ở server |

Môi trường: `frontend/test-results` và `playwright-report` bị khóa quyền, luôn thêm `--output=../data/pw-output`. `frontend/dist` cũ đổi tên `frontend/dist-stale-build2` (ẩn bằng `.git/info/exclude`). Không chèn chú thích `//` vào giữa dòng Java dài; dùng `/* */`.

## 2. Đã hoàn thành

- **Phạm vi ưu đãi** ALL/CATEGORY/BRAND/PRODUCT, migration V11, form khuyến mãi/voucher, nhãn giảm giá (thẻ sản phẩm, trang chi tiết, giỏ hàng, xem trước).
- **Logic**: hoàn lượt voucher khi hủy/hết hạn; "bán chạy" thống nhất; lọc voucher theo danh mục; số sản phẩm chỉ đếm hàng đang bán; cảnh báo khuyến mãi trùng; tìm theo tên/SKU/hãng/danh mục; giỏ hàng báo giá đã đổi.
- **Giao diện khách**: menu hai danh sách, bộ lọc bản nháp + nút "Áp dụng bộ lọc" (không còn bộ lọc bảo hành), trang chủ nhiều khối (24 sản phẩm mới, bộ sưu tập thẻ ngang nhỏ), chân trang xanh, đường dẫn danh mục ở trang chi tiết, "Chưa có đánh giá", nút mua sắm ở đơn hàng trống, nút chat nhỏ trên mobile, vùng bấm ≥ 24 px, khách chưa đăng nhập bấm Thêm vào giỏ hàng thì đăng nhập rồi tự thêm, trang 404 của web.
- **Admin**: form sản phẩm/danh mục/thương hiệu/banner/chính sách/người dùng chia thẻ; danh sách có SKU, danh mục, hãng, nhãn đang giảm, logo, số sản phẩm.
- **P8**: enum `OrderStatus`/`PaymentStatus`, xóa `ProductService` và các hàm repository chết, xóa bộ scaffold TypeScript/Tailwind và trang mock, `open-in-view=false`.
- **Kiểm thử mới**: `BrandCategoryFlowTests` (ma trận hãng ↔ danh mục), `HomeApiTests`, vitest (`frontend/tests-unit`), `filters.spec.js`, `home.spec.js`, `storefront.spec.js`.

## 3. Việc của chủ dự án (không phải mã)

1. **Triển khai lên máy của bạn**: sao lưu schema `techshop` (`mysqldump`), khởi động lại backend cổng 8080 để chạy migration V11, build lại frontend. V11 đổi ưu đãi cũ chọn cả danh mục lẫn hãng thành danh sách sản phẩm cố định.
2. **Xác nhận quy tắc hoàn lượt voucher** khi đơn hủy (trước đây không hoàn). Nếu muốn giữ hành vi cũ: bỏ hai lời gọi `voucherUsage.refund` ở `CommerceService.status` và `SandboxPaymentService.release`, và đổi `docs/API.md` mục "Quy tắc tiền" số 3.
3. **Dọn dữ liệu thử** trong DB: banner `xdfcvsxd`, `gsdffgsd` (ảnh không liên quan), mô tả ngắn giống hệt nhau của sản phẩm demo cũ (bản seed mới đã có mô tả riêng theo danh mục, chỉ áp dụng cho DB mới).
4. **Quyết định kinh doanh còn mở** (vận chuyển do bên thứ ba lo nên phí ship và đơn COD chờ không xử lý trong mã): voucher hiện tính % trên giá đã giảm khuyến mãi (cộng dồn); nếu không muốn cộng dồn cần nói để đổi `CommerceService.totals`.

## 4. Ý tưởng chưa làm (không bắt buộc)

- Trang chủ trên điện thoại hiện 24 sản phẩm mới thành 12 hàng: có thể giới hạn 8 kèm nút "Xem thêm".
- Ảnh và mô tả sản phẩm demo còn là hình minh họa.
