# TechShop AI — Gemini backend

Đã kiểm chứng ngày 30/09/2026: Google chấp nhận khóa local (HTTP 200); gọi thật qua `/api/chat` trả `provider=gemini` và tư vấn laptop thành công. Log `data/gemini-smoke.log` không chứa khóa. Build/package đạt; 10 kiểm thử Java đạt (`data/gemini-tests.log`); hai bài browser chat và banner/ưu đãi đạt (`data/gemini-e2e.log`). Backend JAR đang dùng MySQL techshop tại port 8080 (`data/gemini-server.log`).

- Frontend gọi `POST /api/chat` hoặc `/api/v1/chat`; backend gọi Google qua HTTPS `generateContent`, header `x-goog-api-key`. Không có API key trong Vue, response, URL hoặc bundle.
- Request cũ `{ "message": "Tư vấn laptop" }` vẫn hoạt động. Có thể thêm `history: [{"role":"user|assistant","text":"..."}]`, tối đa 8 tin, mỗi tin 6.000 ký tự; message mới tối đa 1.000 ký tự. Không chấp nhận role system.
- Response `{ "reply": "...", "provider": "gemini|mock" }`. `GET /api/chat/status` (alias `/api/v1/chat/status`) chỉ trả provider được cấu hình, không kiểm tra khả năng xác thực và không trả khóa.
- Không cấu hình khóa hoặc `CHAT_ENABLED=false`: dùng mock cũ. Đã cấu hình Gemini nhưng kết nối lỗi: trả lỗi rõ ràng, không ngầm trả mock. UI giữ câu hỏi để thử lại, không gửi lượt thất bại vào lịch sử.
- Timeout kết nối 5 giây, đọc 30 giây; tối đa 3 request đồng thời trong một JVM và 12 lượt/phút/HTTP session. Khi triển khai nhiều instance/public lớn cần rate limit tại gateway; giới hạn session không phải chống lạm dụng theo danh tính.
- Chat chưa truy vấn kho/đơn hàng: Gemini tư vấn kiến thức thiết bị, không được khẳng định giá/tồn kho/mã giảm giá thực tế. Không gửi tài khoản hoặc đơn hàng sang Google. Nội dung câu hỏi và tối đa 8 tin trước đó được gửi sang Google khi dùng Gemini.

## Cấu hình

Ưu tiên biến môi trường `GEMINI_API_KEY`, tùy chọn `GEMINI_MODEL` (mặc định `gemini-3.5-flash-lite`). Có thể dùng file local `data/chat-local.properties`, đã bị Git ignore:

```properties
shop.chat.api-key=${GEMINI_API_KEY:YOUR_KEY_HERE}
shop.chat.model=${GEMINI_MODEL:gemini-3.5-flash-lite}
```

Thay YOUR_KEY_HERE bằng khóa Copy key từ AI Studio, không chụp/dán khóa vào tài liệu hoặc frontend. Khởi động lại backend sau khi thay cấu hình:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-backend.ps1 -Packaged
```

`401 UNAUTHENTICATED` từ Google nghĩa là chưa xác thực được bằng thông tin đã gửi; kiểm tra khóa được copy đầy đủ và quyền trong AI Studio. `429` là giới hạn sử dụng/quota. Không bật lại fallback để che lỗi xác thực.

## Kiểm tra

1. Mở TechShop AI: cấu hình Gemini hiện nhãn Gemini, không có khóa trong Network.
2. Hỏi “Tư vấn laptop”, sau đó “Khoảng 20 triệu”: gửi lịch sử gần nhất tới backend.
3. Khi lỗi/quota, câu hỏi còn trong input, hiện thông báo và cho gửi lại; không thêm lượt lỗi vào history.
4. Unit/API: `mvnw.cmd "-Dtest=GeminiChatServiceTests,ExperienceApiTests,RestArchitectureTests" test`. Test profile tắt gọi AI thật. Browser: `npm.cmd --prefix frontend run test:e2e -- --grep "Gemini chat"` dùng phản hồi giả lập, không tiêu quota.

Tài liệu đối chiếu: [Gemini API](https://ai.google.dev/api/generate-content), [API keys](https://ai.google.dev/gemini-api/docs/api-key), [Models](https://ai.google.dev/gemini-api/docs/models).
