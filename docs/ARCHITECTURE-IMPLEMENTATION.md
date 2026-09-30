# Thực hiện TechShop Architecture Review

Nguồn yêu cầu: `C:/Users/Admin/Downloads/TechShop Architecture Review.html`, các prompt P0–P8; người dùng yêu cầu triển khai toàn bộ, báo cáo từng phần rồi tiếp tục. Không sửa migration đã áp dụng, không gọi thanh toán hay gửi email thật khi test.

| Phần | Trạng thái | Kết quả |
| --- | --- | --- |
| P0 — Cấu hình, E2E cô lập, dọn và commit | Xong | Default DB không chứa mật khẩu; runner H2/18080 riêng, credentials/upload/mail riêng; ba file rác đã xác minh và gỡ. Backend 45 test (1 bỏ qua) đạt, `npm run build` đạt, E2E H2: 13/14 ca đạt ở lượt đầy đủ, ca upload ảnh (ban đầu kiểm tra sai thư mục ảnh cũ) sửa rồi chạy lại đạt. 4 commit: backend REST + migration, frontend + runner E2E, docs + scripts, dọn file rác |
| P1 — IPN trễ, job hết hạn | Xong | Migration V7 `payment_reference`; IPN 00 đến muộn (đơn CANCELLED/EXPIRED) → `REFUND_REQUIRED`, trả 00, không mở lại đơn/kho; IPN lặp → 02; `PaymentExpiryJob` luôn chạy; admin có cờ, bộ lọc, cảnh báo dashboard; đơn hoàn tiền không lưu trữ được. Backend 52 test đạt (1 bỏ qua; thêm 7), build frontend đạt |
| P2 — Nguồn thời gian khuyến mãi | Xong (một phần có chủ ý) | Đã tái hiện lỗi 7 giờ trên MySQL UTC và sửa: bean `Clock` cho mọi đường Java, `hibernate.jdbc.time_zone` + `SET time_zone` cho MySQL, `compose.yaml` đặt TZ và `--default-time-zone=+07:00`. Giá sale (`@Formula`) vẫn dùng giờ DB (đã cố định múi giờ); chuyển sang Java cùng P7 vì cần snapshot nhận biết transaction (test `@Transactional` đọc giá trong cùng transaction). Test mới: `PromotionClockTests`, `FixedClockTests` (kết quả ở `docs/MIGRATION.md`). Backend 54 test đạt (1 bỏ qua) trên H2; 47 test chọn lọc đạt trên MySQL UTC |
| P3 — Đọc giỏ không khóa, test quy tắc tiền | Chờ | |
| P4 — Gửi lại xác minh | Chờ | |
| P5 — Rate limit, băm token | Chờ | |
| P6 — Tham chiếu, SKU, dashboard | Chờ | |
| P7 — Quy tắc phạm vi dùng chung | Chờ | |
| P8 — DTO/service, enum, code chết, tắt OSIV | Chờ | |

P0–P8 là các phần đang thực hiện. Kết quả kiểm thử và commit của mỗi phần được bổ sung tại đây khi hoàn tất; bảng này không khẳng định các vấn đề chưa triển khai đã được sửa.
