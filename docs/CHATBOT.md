# TechShop AI — backend OpenAI-compatible (Groq)

Cập nhật 04/10/2026: chuyển từ Gemini sang endpoint OpenAI-compatible, mặc định Groq (`https://api.groq.com/openai/v1`, model `openai/gpt-oss-20b`). Giao thức này dùng chung cho Groq, OpenAI và nhiều nhà cung cấp khác nên đổi nhà cung cấp chỉ cần đổi `base-url`, `model` và khóa.

- Frontend gọi `POST /api/chat` hoặc `/api/v1/chat`; backend gọi nhà cung cấp qua HTTPS `POST {base-url}/chat/completions`, header `Authorization: Bearer <key>`. Không có API key trong Vue, response, URL hoặc bundle.
- Request cũ `{ "message": "Tư vấn laptop" }` vẫn hoạt động. Có thể thêm `history: [{"role":"user|assistant","text":"..."}]`, tối đa 8 tin, mỗi tin 1.000 ký tự và tổng lịch sử tối đa 4.000 ký tự (vượt thì trả 400 bằng tiếng Việt; `TechChat.vue` tự bỏ các lượt cũ nhất để vừa giới hạn); message mới tối đa 1.000 ký tự. Không chấp nhận role system từ client; system prompt do backend chèn.
- Response `{ "reply": "...", "provider": "ai|mock" }`. `GET /api/chat/status` (alias `/api/v1/chat/status`) chỉ trả provider được cấu hình, không kiểm tra khả năng xác thực và không trả khóa.
- Không cấu hình khóa hoặc `CHAT_ENABLED=false`: dùng mock cũ. Đã cấu hình khóa nhưng kết nối lỗi: trả lỗi rõ ràng, không ngầm trả mock. UI giữ câu hỏi để thử lại, không gửi lượt thất bại vào lịch sử.
- Timeout kết nối 5 giây, đọc 30 giây; tối đa 3 request đồng thời trong một JVM và 12 lượt/phút mỗi địa chỉ IP (`RateLimitInterceptor`, cả `/api/chat` lẫn `/api/v1/chat` dùng chung một bộ đếm; vượt thì trả 429 kèm `Retry-After`). Bộ đếm nằm trong bộ nhớ của từng instance; khi chạy nhiều instance hoặc public lớn vẫn nên có rate limit ở gateway. Xem mục "Giới hạn tần suất" trong `docs/API.md` về địa chỉ IP sau reverse proxy.
- Chat chưa truy vấn kho/đơn hàng: trợ lý tư vấn kiến thức thiết bị, không được khẳng định giá/tồn kho/mã giảm giá thực tế. Không gửi tài khoản hoặc đơn hàng sang nhà cung cấp. Nội dung câu hỏi và tối đa 8 tin trước đó được gửi đi khi đã cấu hình khóa.

## Cấu hình

Ưu tiên biến môi trường `CHAT_API_KEY`, tùy chọn `CHAT_BASE_URL` và `CHAT_MODEL`. Có thể dùng file local `data/chat-local.properties`, đã bị Git ignore:

```properties
shop.chat.api-key=${CHAT_API_KEY:YOUR_KEY_HERE}
shop.chat.base-url=${CHAT_BASE_URL:https://api.groq.com/openai/v1}
shop.chat.model=${CHAT_MODEL:openai/gpt-oss-20b}
```

Thay YOUR_KEY_HERE bằng khóa lấy từ bảng điều khiển nhà cung cấp, không chụp/dán khóa vào tài liệu hoặc frontend. Danh sách model hiện có: `curl https://api.groq.com/openai/v1/models -H "Authorization: Bearer <key>"`. Khởi động lại backend sau khi thay cấu hình:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-backend.ps1 -Packaged
```

`401` nghĩa là khóa sai hoặc hết hiệu lực; `404` thường là model không còn tồn tại — kiểm tra lại bằng lệnh liệt kê model ở trên. `429` là giới hạn sử dụng/quota. Không bật lại fallback để che lỗi xác thực.

## Kiểm tra

1. Mở TechShop AI: khi đã cấu hình khóa, header hiện "Online"; khi chạy mock hiện "Online · Phản hồi mẫu". Không có khóa trong Network.
2. Hỏi “Tư vấn laptop”, sau đó “Khoảng 20 triệu”: gửi lịch sử gần nhất tới backend.
3. Khi lỗi/quota, câu hỏi còn trong input, hiện thông báo và cho gửi lại; không thêm lượt lỗi vào history.
4. Unit/API: `mvnw.cmd "-Dtest=AiChatServiceTests,ExperienceApiTests,RestArchitectureTests" test`. Test profile tắt gọi AI thật. Browser: `npm.cmd --prefix frontend run test:e2e -- --grep "AI chat"` dùng phản hồi giả lập, không tiêu quota.

Tài liệu đối chiếu: [Groq chat completions](https://console.groq.com/docs/api-reference#chat), [Groq models](https://console.groq.com/docs/models).
