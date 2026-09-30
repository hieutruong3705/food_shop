# TechShop frontend

Vue 3, Vite, Pinia và Vue Router. Cần Node.js 22.12 trở lên.

Ứng dụng nối REST API: `vite.config.js` → `src/main-rest.js` → `RestApp.vue`, router `src/router/index.js` và các trang `*Page.vue`/`TechHome.vue`. Bộ scaffold TypeScript/Tailwind và các trang mock cũ đã bị xóa vì không còn được dùng. `rest.css`, `retail.css`, `catalog.css`, `experience.css`, `admin-layout.css`, `home.css` được nạp theo thứ tự đó; file sau ghi đè file trước, nên đặt tên class riêng cho thành phần mới.

Chạy backend theo [README dự án](../README.md), sau đó tại thư mục này:

```powershell
npm.cmd ci
npm.cmd run dev -- --host 127.0.0.1
```

Mở http://127.0.0.1:5173. Vite proxy API và upload tới backend port 8080.
`VITE_API_BASE_URL` trong `.env.example` cấu hình API client dùng chung; mặc định `/api/v1`.
Xác thực dùng cookie phiên và CSRF. Quyền ADMIN được kiểm tra lại ở backend.

```powershell
npm.cmd run build
npm.cmd run test:unit
npm.cmd run test:e2e
```

`test:unit` chạy vitest (thư mục `tests-unit/`, jsdom) cho logic không cần trình duyệt: `scopeMeta.js`, `FilterRange.vue`, bộ chọn phạm vi. `test:e2e` dùng Playwright, tự dựng backend H2 cô lập ở cổng 18080 từ `../target/techshop.jar`; nếu thư mục `test-results` bị khóa quyền, thêm `--output=../data/pw-output`.

E2E dùng Chrome và hai server đang chạy, tài khoản demo từ `../data/dev-credentials.json`.
Chỉ chạy trên dữ liệu dev/test: test tạo đơn COD, chuyển trạng thái và sửa dữ liệu mẫu.
Không có script lint/typecheck; dự án dùng JavaScript. Kết quả và giới hạn kiểm chứng nằm trong [MIGRATION.md](../docs/MIGRATION.md).
