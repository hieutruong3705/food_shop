# TechShop frontend

Vue 3, Vite, Pinia và Vue Router. Cần Node.js 22.12 trở lên.

Bản hoạt động nối REST API dùng `vite.rest.config.js` → `src/main-rest.js` → `RestApp.vue`, router `src/router/index.js` và các trang `*Page.vue`/`TechHome.vue`. Script `dev`/`build` mặc định dùng entry này.

Bộ TypeScript/Tailwind được thêm độc lập trong quá trình làm việc vẫn giữ nguyên (`main.ts`, `App.vue`, `router/index.ts`, các layout/view TypeScript). Một số trang trong bộ đó còn là scaffold/mock nên chưa dùng làm entry triển khai. Không trộn hai router/store trong cùng ứng dụng. `build:typescript`/`dev:typescript` giữ lệnh riêng cho phần này; không coi kết quả kiểm thử REST là kiểm chứng bộ scaffold. `rest.css` giữ giao diện đã kiểm chứng trước khi stylesheet gốc được thay thế.

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
npm.cmd run test:e2e
```

E2E dùng Chrome và hai server đang chạy, tài khoản demo từ `../data/dev-credentials.json`.
Chỉ chạy trên dữ liệu dev/test: test tạo đơn COD, chuyển trạng thái và sửa dữ liệu mẫu.
Không có script lint/typecheck; dự án dùng JavaScript. Kết quả và giới hạn kiểm chứng nằm trong [MIGRATION.md](../docs/MIGRATION.md).
