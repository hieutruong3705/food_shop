# Schema và dữ liệu

## Dữ liệu kiểm thử trình duyệt

Playwright chỉ chạy backend do `scripts/run-e2e.mjs` khởi động: profile `dev,e2e`, cổng 18080, H2 file `data/techshop-e2e`. Database này dùng create-drop và Flyway tắt riêng cho test H2; MySQL giữ Flyway và validate. Script ép cấu hình datasource bằng đối số CLI, nên DB_URL hoặc cấu hình MySQL của máy không thể chuyển test sang schema đang dùng. Không đặt `E2E_URL` về 8080 hoặc một máy chủ thật.

Nếu muốn dọn fixture cũ trong MySQL, thực hiện riêng sau khi kiểm tra và sao lưu:

```powershell
# Dùng thông tin tài khoản trong client config cục bộ, không đặt mật khẩu trong lệnh.
mysqldump --defaults-extra-file=data/mysql-backup-client.cnf --single-transaction --no-tablespaces --result-file=data/backups/techshop-before-fixture-cleanup.sql techshop
node scripts/hide-test-fixtures.mjs
# Đối chiếu danh sách preview và bản sao lưu rồi mới chạy:
node scripts/hide-test-fixtures.mjs --apply
```

Script chỉ ẩn sản phẩm khớp chính xác mẫu test đã biết và tự lưu snapshot JSON; không xóa đơn hàng hay dữ liệu kinh doanh. Không có thao tác dọn MySQL tự động trong E2E.

## Múi giờ MySQL

Ngày giờ (khuyến mãi, voucher, đơn hàng) lưu theo giờ Việt Nam. Ứng dụng tự đặt `hibernate.jdbc.time_zone=Asia/Ho_Chi_Minh` và `SET time_zone = '+07:00'` cho mọi kết nối MySQL/MariaDB, nên kết quả không phụ thuộc múi giờ của JVM hay container. MySQL production vẫn nên chạy với `default-time-zone='+07:00'` (mục `command` của service `mysql` trong `compose.yaml` đã đặt sẵn, cùng `TZ=Asia/Ho_Chi_Minh`) để các công cụ khác như báo cáo SQL thấy cùng một giờ. Nếu ghi đè `spring.datasource.hikari.connection-init-sql`, hãy giữ lệnh đặt múi giờ. Xem kết quả tái hiện lỗi lệch 7 giờ trong `docs/MIGRATION.md`.

## Local hiện tại — MySQL techshop (29/09/2026)

Ứng dụng đã chuyển từ H2 sang MySQL80 tại `127.0.0.1:3306/techshop`. Profile `dev,mysql-local` dùng MySQL Connector, Flyway V1–V5 và `ddl-auto=validate`; vẫn dùng email local và tài khoản demo. Script `run-backend.ps1` mặc định chọn MySQL.

Kết nối được lưu trong tệp ignored `data/mysql-local.properties`:

```properties
DB_URL=jdbc:mysql://127.0.0.1:3306/techshop?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=techshop_local
DB_PASSWORD=<mật khẩu tài khoản MySQL riêng>
```

Tài khoản ứng dụng chỉ có quyền trên `techshop.*`. Tệp cấu hình không nằm trong Git; không đổi mật khẩu tài khoản MySQL hiện có. Khi clone sang máy khác, tạo schema/tài khoản phù hợp và tệp trên trước khi chạy.

Đã sao lưu schema MySQL ban đầu (rỗng) và tệp H2 ngoại tuyến tại `data/backups/mysql-switch-20260929-071250`. Sau đó chạy `scripts/MigrateLocalData.java`: tạo schema bằng Flyway, chép 21 bảng trong transaction, giữ khóa ngoại bật, so sánh toàn bộ giá trị và số dòng trước commit. Kết quả: 37 sản phẩm, 8 biến thể, 40 ảnh, 2 tài khoản, 9 đơn và 9 dòng đơn. Tệp ảnh vẫn ở `data/uploads`; mật khẩu tài khoản và các ID được giữ nguyên. Log: `data/mysql-data-transfer.log`.

Công cụ chỉ nhận bản sao H2 ngoại tuyến và MySQL local `techshop` chưa có dữ liệu; từ chối nhập đè hoặc bỏ sót bảng/cột. Không chạy lại trên database đang dùng. Nếu cần chuyển một môi trường trống khác: sao lưu, dừng server nguồn, tạo bản sao H2, tạo cấu hình riêng, chuẩn bị JDBC/Flyway classpath bằng Maven `dependency:build-classpath`, rồi chạy Java source launcher với `scripts/MigrateLocalData.java <đường-dẫn-bản-sao-không-có-.mv.db>`. Không dùng công cụ này để ghép hai database đã có dữ liệu.

Tệp H2 gốc được giữ nguyên, không đồng bộ sau chuyển. `-Database h2` chỉ phục vụ mở lại nguồn cũ. Trong MySQL Workbench, refresh schema `techshop` → Tables để thấy bảng và dữ liệu.

## Database mới

Tạo database MySQL 8 với charset `utf8mb4`, cấp quyền cho tài khoản ứng dụng, truyền `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Flyway chạy V1 (schema cũ), V2 (catalog và checkout), V3 (lưu trữ đơn admin), V4 (điều kiện voucher và ghi chú đơn); Hibernate chỉ validate. Không có tài khoản admin mặc định ngoài profile dev. Với môi trường thật, đăng ký/xác minh tài khoản rồi người quản trị database cấp role ADMIN cho đúng email.

```sql
CREATE DATABASE techshop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Tạo tài khoản ứng dụng với mật khẩu do bạn chọn và cấp quyền trên techshop.*.
```

## Database cũ

1. Sao lưu database và tệp upload; thử quy trình trên bản sao trước.
2. Đối chiếu schema với V1. V1 mô tả mô hình trước chuyển đổi, không được chạy `CREATE TABLE` lại trên schema đang có dữ liệu.
3. Chỉ với schema tương thích V1 và chưa có Flyway history, đặt `DB_BASELINE=true` cho lần khởi động migration đầu. Flyway baseline ở 1 rồi chạy V2. Sau đó bỏ biến này hoặc đặt lại false.
4. V2 thêm SKU/slug/bảo hành, cờ hiển thị catalog công nghệ, trường idempotency và snapshot đơn hàng, hạn token và index. Không xóa bản ghi hay đổi tên sản phẩm thực phẩm thành công nghệ.
5. V2 chụp tên hiện có vào dòng đơn cũ. Nếu tên từng bị thay đổi trước migration, không thể khôi phục chính xác tên lịch sử từ dữ liệu hiện tại.
6. Token cũ không có hạn dùng sẽ không còn hợp lệ; người dùng yêu cầu reset mới. Với tài khoản chưa xác minh từ hệ thống cũ, quản trị viên xác minh lại trước khi kích hoạt.
7. Duyệt nội dung trong trang quản trị, bật cờ catalog công nghệ cho những mục phù hợp. Đơn cũ vẫn được đọc bằng API/Vue.
8. V3 chỉ thêm cờ lưu trữ đơn; V4 thêm các điều kiện voucher với mặc định tương thích (active, minimum=0, không giới hạn ngày/lượt) và note nullable. Không suy diễn số lần dùng voucher trong đơn cũ; `used_count` bắt đầu đếm từ V4.

Không bật `DB_BASELINE` nếu database đã có các cột V2 do từng chạy Hibernate update; cần kiểm tra và lập migration riêng cho trạng thái đó. MySQL DDL tự commit: khi có lỗi giữa migration, khôi phục bản sao hoặc sửa có kiểm soát rồi mới dùng Flyway repair. Không tự động rollback bằng cách xóa cột/dữ liệu.

## Các môi trường

V5 thêm `product_variants`, `variant_options`, liên kết `variant_id` nullable ở giỏ/chi tiết đơn, snapshot `variant_label` và thứ tự `product_images.sort_order`. Không chuyển sản phẩm cũ thành phiên bản, không xóa dữ liệu hoặc thay đổi giá/kho của chúng. Dòng đơn cũ giữ liên kết phiên bản rỗng. Database mới chạy đủ V1–V5; database baseline V1 chạy V2–V5.

| Môi trường | Database | Tạo schema | Seed/email |
|---|---|---|---|
| Mặc định | MySQL từ DB_URL | Flyway + validate | Không demo; SMTP cấu hình riêng |
| dev | H2 file `data/techshop-demo` | Hibernate update | Demo opt-in trong profile; email cục bộ |
| dev,mysql-local (mặc định script local) | MySQL80 / techshop | Flyway + validate | Tài khoản/dữ liệu đã chuyển; email cục bộ |
| test | H2 memory | create-drop | Fixtures riêng; mail mock |
| script test-mysql | MySQL riêng port 3307/schema techshop_test | Flyway + validate | Fixtures riêng; mail mock |
| Docker demo | MySQL container + volume riêng | Flyway + validate | Demo và email cục bộ |

Seed nhận diện SKU `TECH-DEMO-*`, không xóa dữ liệu đã tồn tại và không đặt lại mật khẩu. Không bật profile dev trên database thật.
