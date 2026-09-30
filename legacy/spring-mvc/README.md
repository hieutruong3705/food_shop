# Mã Spring MVC/JSP đã thay thế

Thư mục này lưu nguyên mã nguồn cũ để đối chiếu, không nằm trong source set Maven, component scan hoặc JAR triển khai. Không chạy hoặc sao chép lại controller từ đây vào `src/main/java` vì các luồng API mới đã thay thế chúng.

- Controller đang dùng: `src/main/java/com/example/shop/api/controller`.
- Quản lý người dùng/sản phẩm/thương hiệu và các module admin: `AdminApiController` → `AdminCatalogService`, Vue `AdminPage`.
- Mua hàng: `CartApiController`, `OrderApiController` → `CommerceService`.
- Thanh toán: `PaymentApiController` → `SandboxPaymentService`.
- Đăng nhập/hồ sơ: `AuthApiController`, `AccountApiController`.
- Catalog, đánh giá, chính sách và upload có các REST controller tương ứng.
- `SpaController` chỉ phục vụ `index.html` của Vue; không xử lý nghiệp vụ hoặc trả JSP.

Các service chỉ được MVC cũ sử dụng cũng được lưu tại đây. Những service dùng chung với REST vẫn ở nguồn chính. Ảnh trong `src/main/resources/static` được giữ vì URL có thể còn nằm trong database. Ma trận chức năng và kết quả kiểm thử ở `docs/MIGRATION.md`, hợp đồng API ở `docs/API.md`.
