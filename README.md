# TechShop

Bản cập nhật mega menu, slider banner DB, khuyến mãi/voucher và chatbot mock: [kết quả, file thay đổi, API và hướng dẫn test nhanh](docs/TECHSHOP-COMPLETION.md).

Website bán thiết bị công nghệ, giao diện tiếng Việt. Backend Spring Boot 3.5 / Java 17 cung cấp REST API; frontend Vue 3 / Vite / Pinia / Vue Router. Môi trường local hiện dùng MySQL 8, schema `techshop` tại `127.0.0.1:3306`. H2 được giữ làm bản nguồn/tuỳ chọn dự phòng.

## Chạy trên Windows

Hướng dẫn [quản lý icon danh mục, logo hãng và bộ lọc thông số](docs/CATALOG-UI.md). Có thể tải ảnh trực tiếp trong admin; bộ lọc tự lấy thông số của sản phẩm theo từng danh mục.

Giao diện REST có bố cục bán lẻ tông xanh ngọc: thanh tìm kiếm, menu danh mục, banner chuyển bộ sưu tập, lọc sản phẩm nổi bật, nhãn giảm giá theo API và lưới sản phẩm thích ứng điện thoại. Thành phần dùng chung nằm ở `frontend/src/RestApp.vue`, giao diện trang chủ ở `frontend/src/views/TechHome.vue`, lớp CSS mới ở `frontend/src/retail.css`. Thiết kế tham khảo bố cục Hoàng Hà Mobile, giữ thương hiệu và dữ liệu TechShop.

Cần JDK 17 trở lên và Node.js 22.12 trở lên. Maven Wrapper có sẵn.

Terminal thứ nhất, tại thư mục dự án:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/run-backend.ps1
```

Script mặc định dùng profile `dev,mysql-local`, đọc kết nối từ `data/mysql-local.properties` (không commit). MySQL80 cần chạy. Dùng thêm `-Packaged` để chạy JAR đã build và mở **http://127.0.0.1:8080**, không cần Vite. Chỉ dùng `-Database h2` nếu chủ động muốn mở database H2 cũ; hai database không đồng bộ tự động.

Terminal thứ hai (khi phát triển giao diện):

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev -- --host 127.0.0.1
```

Mở **http://127.0.0.1:5173**. Backend ở port 8080; Vite proxy `/api`, `/images`, `/uploads` sang backend.

- Tài khoản demo: `admin@techshop.test` và `user@techshop.test`. Mật khẩu ngẫu nhiên được tạo ở `data/dev-credentials.json`; không commit hoặc chia sẻ tệp này.
- Demo có 35 sản phẩm minh họa trong 8 danh mục, 2 thương hiệu, banner, khuyến mãi Orbit Demo, voucher `TECH10` và chính sách mẫu. Sản phẩm “Điện thoại Orbit nhiều phiên bản” có màu Đen/Trắng và dung lượng 128/256 GB. Seed chạy lại không nhân đôi sản phẩm hoặc đặt lại mật khẩu.
- Database đang dùng: MySQL `techshop`. Dữ liệu H2 cũ đã chuyển ngày 29/09, giữ nguyên ID, tài khoản, ảnh, biến thể và đơn hàng. Bản sao lưu ở `data/backups/mysql-switch-20260929-071250`.
- Email trong profile `dev` được lưu tại `data/dev-mail/*.eml`; mở tệp mới nhất để lấy liên kết xác minh/đặt lại mật khẩu. Profile này không gửi SMTP thật.
- Dữ liệu demo không đại diện thông số hoặc giá của sản phẩm thương mại. Sản phẩm, ảnh và phiên bản được lưu qua API vào database, không phải dữ liệu giả ở frontend.

## MySQL và dữ liệu hiện có

Xem [quy trình migration](docs/DATABASE.md) trước khi kết nối database đã có dữ liệu. Cấu hình mẫu ở [.env.example](.env.example); Spring đọc biến môi trường, không tự đọc file `.env` khi chạy Maven.

Mặc định ứng dụng dùng MySQL và `ddl-auto=validate`; Flyway quản lý schema. Database mới chạy V1–V5. Database cũ cần sao lưu, kiểm tra cấu trúc và baseline phiên bản 1 trước khi chạy V2–V5. Không dùng `create` hoặc `create-drop` với dữ liệu thật.

Nội dung cũ được giữ nguyên nhưng mặc định không xuất hiện trong catalog công nghệ. Quản trị viên duyệt sản phẩm, banner, chính sách rồi bật **Hiển thị trong cửa hàng công nghệ**. Tên sản phẩm, đơn hàng và dữ liệu lịch sử không bị chuyển nghĩa tự động.

## Kiểm thử

```powershell
.\mvnw.cmd test
cd frontend
npm run build
npm run test:e2e
```

Nếu PowerShell chặn `npm.ps1`, dùng `npm.cmd` (không cần đổi execution policy toàn máy). Script `.ps1` có thể chạy bằng `powershell.exe -NoProfile -ExecutionPolicy Bypass -File <script>`.

Test Java mặc định dùng H2 cô lập. Trước E2E, chạy `npm.cmd --prefix frontend run build` và `.\mvnw.cmd package`. Playwright tự mở backend `dev,e2e` tại `127.0.0.1:18080`, với H2 file `data/techshop-e2e` được tạo lại mỗi lượt; không dùng MySQL `techshop`. Không cần mở Vite/backend dev. Chỉ URL 18080 được chấp nhận và không tái sử dụng server đang chạy. Tài khoản ngẫu nhiên nằm trong `data/e2e-credentials.json`, mail và ảnh test ở `data/e2e-mail`/`data/e2e-uploads`; tất cả đã ignore. Playwright dùng Chrome cài sẵn, không gọi Gemini, SMTP hoặc VNPay thật. Ảnh và báo cáo ở `frontend/test-results` và `frontend/playwright-report`.

Kiểm thử MySQL thật với instance riêng, nếu máy có MySQL Server 8 tại đường dẫn mặc định:

```powershell
.\scripts\start-test-mysql.ps1
.\scripts\test-mysql.ps1
.\scripts\test-legacy-migration.ps1
```

Instance này chỉ lắng nghe `127.0.0.1:3307`, dữ liệu ở `data/mysql-test`, schema `techshop_test`. Script test chặn tên schema/port khác, chạy migration và Hibernate validation. Test migration cũ tạo một schema có tên ngẫu nhiên trong instance riêng để kiểm tra baseline, dữ liệu lịch sử và chạy lại an toàn. Không chạy script khởi tạo lần nữa khi instance đã tồn tại. Có thể dừng bằng script `stop-test-mysql.ps1`.

Lượt kiểm thử biến thể dùng instance riêng `mysql-variants-test`: truyền `-Instance mysql-variants-test` vào các script trên và script dừng. Khởi động lại instance đã có bằng `start-test-mysql.ps1 -Instance mysql-variants-test -RestartExisting`; dữ liệu/mật khẩu được giữ nguyên. Chỉ dùng `-ResumeInitialized` khi lần khởi tạo trước đã tạo tệp database nhưng chưa cấu hình tài khoản.

## API và quy tắc nghiệp vụ

Backend hiện chỉ dùng REST controller cho nghiệp vụ. MVC/JSP và các service riêng của luồng cũ đã chuyển sang `legacy/spring-mvc`, không biên dịch hoặc đóng gói vào JAR. `SpaController` chỉ phục vụ Vue. File `UserController.java` cũ được lưu ở thư mục này; chức năng quản trị người dùng hiện nằm trong `api/controller/AdminApiController.java` và `api/service/AdminCatalogService.java`.

Frontend build dùng `vite.config.js` với entry `src/main-rest.js`. Bộ scaffold TypeScript/Tailwind và các trang mock cũ đã được xóa (30/09/2026). Xem `frontend/README.md`.

Nhãn khuyến mãi hiển thị phần trăm cụ thể (ví dụ **Giảm 5%**) trên thẻ sản phẩm và chi tiết, tính từ giá niêm yết/giá sau giảm do API trả về; khi chọn phiên bản, nhãn cập nhật theo giá phiên bản.

Trong **Quản trị → Sản phẩm → Thêm mới/Sửa**, phần **Tùy chọn sản phẩm** cho phép thêm phiên bản với thuộc tính tự đặt (Màu sắc, Dung lượng, Phiên bản…), SKU, giá, tồn kho và ảnh riêng. Mỗi sản phẩm tối đa 100 phiên bản, mỗi phiên bản 1–6 thuộc tính. Giá chung lấy mức thấp nhất, kho chung cộng các phiên bản đang bán. Ngừng phiên bản bằng ô Đang bán để giữ lịch sử. Sản phẩm có đơn thường đang xử lý cần hoàn tất/hủy các đơn đó trước khi chuyển sang có phiên bản.

Chọn tệp ảnh để tải lên và xem trước, sau đó bấm **Lưu thay đổi** để lưu liên kết ảnh vào sản phẩm. Có thể chọn ảnh đại diện, bỏ ảnh khỏi sản phẩm hoặc tải ảnh riêng cho phiên bản. Tệp nằm trong `data/uploads`, URL và thứ tự ảnh nằm trong database. Bỏ ảnh khỏi sản phẩm không xóa tệp đang có thể được dùng ở nơi khác.

Khách phải chọn phiên bản trước khi thêm vào giỏ; giá, ảnh, SKU và tồn kho đổi theo lựa chọn. Giỏ tách riêng từng phiên bản. Đơn hàng lưu snapshot tùy chọn/SKU/giá; hủy hoặc thanh toán thất bại hoàn đúng kho phiên bản một lần.

Trang chi tiết chia ô chọn theo từng thuộc tính. Ô màu có ảnh thu nhỏ, ô đang chọn có viền đỏ và dấu tích; cấu hình không có sẵn bị khóa. Đổi màu sẽ bỏ lựa chọn cấu hình không tương thích. Chữ phụ, menu, form và bảng đã tăng nhẹ; nội dung chữ nhỏ tối thiểu 12px (trừ dấu tích trang trí).

Xem [docs/API.md](docs/API.md). Dùng cookie phiên và CSRF; frontend luôn gửi credentials. USER chỉ truy cập dữ liệu của mình; ADMIN được kiểm tra ở backend. Giá khuyến mãi, voucher và phí giao hàng đều tính ở server. Đơn từ 1 triệu đồng được miễn phí vận chuyển, còn lại 30.000₫. Khuyến mãi chọn mức giảm cao nhất, voucher tiếp tục áp dụng trên các dòng đủ điều kiện. Giá VND được làm tròn đến đồng.

Checkout yêu cầu `Idempotency-Key`; khóa người mua và sản phẩm trong transaction, đọc lại tồn kho bằng locking read để tương thích MySQL REPEATABLE READ. Hủy đơn hoàn tồn kho một lần. Thông tin tên/SKU/đơn giá được lưu vào dòng đơn hàng.

Voucher hỗ trợ bật/tắt, thời gian hiệu lực, đơn tối thiểu, giới hạn giảm và số lượt dùng. Backend khóa voucher khi tính/đặt đơn; retry không tiêu thụ thêm lượt. Lượt dùng tính theo đơn đã tạo thành công, không hoàn lại khi hủy hoặc thanh toán thất bại. Voucher cũ không có giới hạn vẫn giữ hành vi cũ. Ghi chú giao hàng tối đa 1.000 ký tự xuất hiện ở chi tiết đơn khách và admin.

Upload tại `/api/v1/admin/uploads`: PNG/JPEG/WebP tối đa 5 MB, kiểm tra chữ ký tệp, MIME và đuôi; lưu ở `UPLOAD_DIR` (mặc định `data/uploads`) với tên ngẫu nhiên, phục vụ qua `/uploads`. Ảnh lỗi có fallback trên Vue.

## VNPay sandbox

Mặc định tắt. Cần `VNPAY_ENABLED=true`, `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` do VNPay sandbox cấp, cùng `CLIENT_URL` của frontend. Chỉ cho phép endpoint sandbox; không có cấu hình thanh toán live.

Đăng ký IPN `/api/v1/payments/vnpay/ipn` với VNPay. IPN kiểm tra HMAC, merchant, mã đơn, số tiền và trạng thái trước khi cập nhật. Trang return không tự đánh dấu đã thanh toán. Đơn chưa thanh toán hết hạn sau 20 phút và hoàn kho; link thanh toán có hạn 15 phút. Job hết hạn luôn chạy, kể cả khi tắt VNPay, để đơn VNPAY chưa trả không giữ kho vĩnh viễn. Đơn online đã thanh toán không được hủy bằng luồng COD; cần quy trình hoàn tiền riêng trước khi triển khai thanh toán live.

IPN hợp lệ báo đã thu tiền nhưng đến sau khi đơn đã bị hủy hoặc hết hạn: hệ thống trả `RspCode 00` để VNPay ngừng gửi lại, đặt `paymentStatus=REFUND_REQUIRED`, lưu mã giao dịch VNPay vào `payment_reference` và không mở lại đơn hay trừ lại kho (kho có thể đã bán cho người khác). Admin thấy cảnh báo "Đơn cần hoàn tiền" trên bảng điều khiển, lọc được ở danh sách đơn, và đơn này không có thao tác chuyển trạng thái hay lưu trữ. Quy trình hoàn tiền vẫn làm thủ công trên cổng VNPay.

Không có giao dịch với VNPay thật trong kiểm thử. Test tạo chữ ký giả lập cục bộ. Tham khảo [tài liệu tích hợp VNPay](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html).

## Build và Docker

```powershell
cd frontend
npm ci
npm run build
cd ..
.\mvnw.cmd package
```

`target/techshop.jar` chứa Vue đã build. Có thể chạy cùng origin bằng `java -jar target/techshop.jar` với các biến môi trường MySQL; route refresh của Vue được backend phục vụ. Kiểm tra sạch đầy đủ: `.\mvnw.cmd clean package`.

Docker Compose tạo môi trường demo MySQL riêng, backend và frontend cùng origin. Sao chép `.env.example` thành `.env`, đặt mật khẩu database và demo rồi chạy `docker compose up --build`. Mở http://localhost:8080. Không dùng mật khẩu demo hay profile `dev` trên production.

Chưa tự triển khai lên dịch vụ bên ngoài. Trạng thái kiểm chứng và giới hạn còn lại được ghi tại [docs/MIGRATION.md](docs/MIGRATION.md).
