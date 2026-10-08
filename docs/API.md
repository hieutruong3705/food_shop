# REST API /api/v1

JSON UTF-8. Public GET không cần đăng nhập; API được bảo vệ trả 401/403 JSON thay vì redirect HTML. Lỗi validation có `message`, `status`, `details` theo tên trường. Tất cả request thay đổi dữ liệu cần CSRF.

## Phiên và CSRF

Gọi `GET /auth/csrf`, giữ cookie và đọc `{headerName,token}`. Gửi token trong header được trả về khi POST/PUT/PATCH/DELETE. Đăng nhập tạo session cookie HttpOnly. Không lưu mật khẩu/token đăng nhập vào localStorage. CORS chỉ cho origin cấu hình bởi `CLIENT_URL`.

| Method | Route | Nội dung |
|---|---|---|
| POST | /auth/register | firstName,lastName,email,password,confirmPassword. Tài khoản được lưu trước khi gửi mail; lỗi SMTP chỉ ghi log WARN (không có mã) và vẫn trả 201. Email đã có tài khoản chờ xác minh mà link quá 24 giờ thì đăng ký lại được (thay họ tên, mật khẩu và mã); tài khoản đã xác minh, bị khóa hoặc bị admin vô hiệu hóa vẫn báo trùng |
| POST | /auth/login | email,password → thông tin người dùng |
| POST | /auth/logout | Kết thúc phiên |
| GET | /auth/me | Thông tin phiên hiện tại |
| POST | /auth/resend-verification | `{ "email": "..." }`. Cấp mã mới 24 giờ và gửi lại cho tài khoản đang chờ xác minh, tối đa 1 lần mỗi 60 giây mỗi tài khoản (`verification_sent_at`). Luôn trả 200 cùng một thông điệp, dù email không tồn tại, đã xác minh, bị khóa hay đang bị giới hạn. Mã cũ hết hiệu lực khi cấp mã mới; yêu cầu CSRF |
| POST | /auth/verify | JSON `{ "code": "..." }`; token 24 giờ, dùng một lần; yêu cầu CSRF |
| POST | /auth/forgot-password | email; phản hồi không tiết lộ email tồn tại |
| POST | /auth/reset-password | token,password,confirmPassword; hạn 1 giờ |
| GET, PUT | /account | fullName,phone,address |
| POST | /account/password | currentPassword,password; kết thúc phiên hiện tại |

## Giới hạn tần suất và token email

Giới hạn theo địa chỉ IP (`request.getRemoteAddr()`), đếm trong bộ nhớ từng instance, cửa sổ trượt. Vượt giới hạn trả `429` với `{timestamp,status,message,details}` như mọi lỗi API cùng header `Retry-After` (giây):

| Endpoint (POST) | Giới hạn |
|---|---|
| `/api/chat`, `/api/v1/chat` (một bộ đếm chung) | 12 lượt/phút |
| `/auth/login` | 10 lượt/phút; ngoài ra mỗi email sai mật khẩu 5 lần liên tiếp thì khóa 5 phút (kể cả email không có tài khoản, và cả khi nhập đúng mật khẩu trong thời gian khóa). Lỗi cũ hơn 15 phút không cộng dồn; đăng nhập thành công xóa bộ đếm |
| `/auth/register`, `/auth/forgot-password`, `/auth/resend-verification` | 5 lượt/10 phút, đếm riêng cho từng endpoint |

Tắt bằng `RATE_LIMIT_ENABLED=false` (profile `test` và `e2e` đã tắt vì bộ kiểm thử đăng nhập rất dày từ một địa chỉ). **Reverse proxy:** `X-Forwarded-For` không bao giờ được tin trực tiếp. Sau proxy, mọi khách sẽ trông như cùng một IP (địa chỉ của proxy) và dùng chung giới hạn, nên hãy đặt `server.forward-headers-strategy=native` cùng `server.tomcat.remoteip.internal-proxies=<địa chỉ proxy>` để Tomcat chỉ tin header từ proxy đó.

Mã xác minh email và token đặt lại mật khẩu là 256 bit ngẫu nhiên gửi qua email; database chỉ lưu SHA-256 (hex 64 ký tự) của chúng. Migration V9 xóa token đặt lại mật khẩu cũ và thay mã xác minh cũ của các tài khoản đang chờ bằng giá trị ngẫu nhiên: **mọi liên kết xác minh và đặt lại mật khẩu đã gửi trước khi triển khai bản này ngừng hoạt động**; người dùng bấm "Gửi lại email xác minh" hoặc "Quên mật khẩu" để nhận liên kết mới.

## Mua hàng

`GET /catalog/menu?categoryId=...` trả facets cùng `bestSellers: [{product,units}]` (tối đa 10; **rỗng** khi danh mục chưa có đơn) và `newest: [{product,units:0}]` (5 sản phẩm mới nhất đang bán, còn hàng trước). `rankingSource: sales|newest` chỉ giữ cho client cũ. Tổng bán từ order_details của đơn CONFIRMED/SHIPPING/COMPLETED, COD hoặc PAID.

`GET /home` (công khai) trả các khối trang chủ: `promotion` (khuyến mãi đang chạy có tỉ lệ cao nhất còn sản phẩm bán, kèm `products` tối đa 8, `endDate`, `scopeDescription`; `null` nếu không có), `brands` (hãng có sản phẩm bán, kèm `count`), `bestSellers` (8, cùng cách đếm với menu), `categoryRows` (3 danh mục nhiều sản phẩm bán nhất × 4 sản phẩm), `reviews` (4 đánh giá 4–5 sao mới nhất: `rating,content,author,productId,productName,createdAt`, không có email). Kết quả được giữ `shop.home-cache-seconds` giây (mặc định 30; profile test/e2e đặt 0). Mỗi dòng của `GET /cart` có `price` (giá hiện tại, đã trừ khuyến mãi), `listPrice` (giá niêm yết), `discountRate` (% khuyến mãi đang áp dụng, 0 nếu không) và `addedPrice` (giá lúc khách thêm vào giỏ; giao diện báo nếu khác `price`).

`GET /offers` trả promotion/voucher đủ điều kiện công khai; `GET /products?promotionId=...` lọc sản phẩm áp dụng (kết hợp với category/brand/stock/spec/price).

`GET /offers` trả toàn bộ mã đủ điều kiện để luân phiên (không cắt còn 6 mã). Promotion có thêm `hasAvailableProducts`: false khi các sản phẩm áp dụng chưa mở bán; vẫn trả mức giảm của chương trình active, còn hạn và có cấu hình phạm vi. Giao diện hiện trạng thái này và không dẫn đến danh sách rỗng. Giá bán/checkout vẫn tính theo sản phẩm và kiểm tra điều kiện ở backend.

Ảnh ưu đãi và vị trí banner:

- `GET /offers` và `GET /home` trả thêm `imageUrl` cho promotion và voucher (ảnh admin upload qua `POST /api/v1/admin/uploads`); không có ảnh thì trả chuỗi rỗng, không trả `null`.
- Banner có `placement`: `HOME_HERO` (mặc định, băng chuyền đầu trang chủ), `VOUCHER_PAGE` (trang mã giảm giá), `HOME_BANNER_1`–`HOME_BANNER_3` (banner ngang trang chủ), `HOME_BRAND_1_1`–`HOME_BRAND_1_4` (chuyên trang thương hiệu), `HOME_GROUP_1`–`HOME_GROUP_5` (banner quảng cáo ở đầu từng nhóm "Sản phẩm mới": nhóm 1, 3, 5 chiếm 2 ô sản phẩm – ảnh 970×884; nhóm 2, 4 chiếm 1 ô – ảnh 470×884), `HOME_RECENT` (banner 1 ô ở cuối hàng "Bạn vừa xem" – ảnh 470×884). `GET /banners` không truyền tham số vẫn chỉ trả banner `HOME_HERO` như trước; `GET /banners?placement=...` trả banner của vị trí đó, giá trị không hợp lệ trả 400.
- Admin thêm `imageUrl` cho promotion/voucher và `placement` cho banner; banner lưu không kèm `placement` mặc định về `HOME_HERO`.

Chat AI: `POST /api/chat` (alias `/api/v1/chat`) nhận `message` và `history` tùy chọn (tối đa 8 tin, role user/assistant), trả `reply` và `provider`. `GET /api/chat/status` hoặc `/api/v1/chat/status` chỉ trả provider. Không cấu hình khóa thì dùng mock; lỗi từ nhà cung cấp không ngầm chuyển sang mock. CSRF vẫn bắt buộc khi POST. Cấu hình và giới hạn: [CHATBOT.md](CHATBOT.md).

Chat public vẫn cần CSRF lấy qua `/api/v1/auth/csrf`, message 1–1.000 ký tự không trắng. Không có AI key trong frontend.

`GET /products/{id}/reviews/summary` trả `average`, `count`, `purchased`, `canReview`; khách chưa đăng nhập vẫn xem tổng hợp, không được đánh giá. Quyền đánh giá yêu cầu đơn COMPLETED và chưa có review của user cho sản phẩm.

Order response thêm `allowedStatuses`, tính cùng quy tắc với service chuyển trạng thái (bao gồm thanh toán). Đây là danh sách thao tác cho admin; người mua không được cấp thêm quyền. `PATCH /admin/orders/{id}/status` vẫn kiểm tra ADMIN, CSRF và khóa đơn trước cập nhật.

`PATCH /admin/products/{id}/status` (ADMIN + CSRF): `{ "active": true }` để bán lại, `false` để ngừng bán. Chỉ thay trạng thái kinh doanh, khóa bản ghi sản phẩm như thao tác tồn kho; giữ SKU, ảnh, giá, tồn kho và trạng thái từng phiên bản. Gửi lại cùng trạng thái không gây đảo trạng thái. `DELETE /admin/products/{id}` vẫn ngừng bán để tương thích.

Danh sách sản phẩm công khai và quản trị luôn ưu tiên `quantity > 0` trước `quantity = 0`, thực hiện trong truy vấn database trước phân trang. Trong từng nhóm, giữ thứ tự giá/mới nhất/bán chạy đã chọn (quản trị: mới nhất), có ID làm thứ tự phụ ổn định. Các khối sản phẩm trên trang chủ và sản phẩm liên quan dùng chung API này.

| Method | Route | Nội dung |
|---|---|---|
| GET | /products | search,categoryId,brandId,brandIds,minPrice,maxPrice,inStock,spec.{tên},sort,page,size |
| GET | /catalog/facets?categoryId=... | Hãng kèm logo, khoảng giá, số lượng còn hàng và nhóm thông số riêng của danh mục |
| GET | /products/{id} | Sản phẩm, ảnh, thông số, SKU và bảo hành |
| GET | /categories, /brands, /banners, /policies | Nội dung đã duyệt cho catalog công nghệ; /banners nhận `placement` tùy chọn (`HOME_HERO` mặc định; các vị trí khác xem mục "Ảnh ưu đãi và vị trí banner") |
| GET | /policies/{id} | Chi tiết chính sách đang hiển thị |
| GET, POST | /products/{id}/reviews | POST: rating 1–5,content; yêu cầu đơn COMPLETED |
| PUT, DELETE | /products/{id}/reviews/{reviewId} | Chỉ chủ đánh giá được sửa/xóa; GET trả editable theo phiên |
| GET | /cart | Dòng giỏ hàng, tồn kho, giá hiện tại |
| POST | /cart/items | productId,quantity 1–100,variantId (bắt buộc khi sản phẩm có phiên bản) |
| PATCH | /cart/items/{id} | quantity |
| DELETE | /cart/items/{id} | Xóa dòng thuộc người dùng hiện tại |
| GET | /cart/quote?voucherCode=... | Tổng tiền server tính; có thể truyền cartItemIds dạng danh sách phân cách bằng dấu phẩy |
| POST | /orders | receiverName,receiverAddress,receiverPhone,voucherCode,paymentMethod,cartItemIds và note (tùy chọn, tối đa 1.000 ký tự) |
| GET | /orders, /orders/{id} | Đơn thuộc người dùng hiện tại |
| PATCH | /orders/{id}/status | `{ "status": "CANCELLED" }`; chỉ khi PENDING, chưa trả tiền |
| GET | /payments/options | COD và trạng thái cấu hình VNPay sandbox |
| POST | /orders/{id}/payment | URL VNPay sandbox cho đơn online chưa thanh toán |
| GET | /payments/vnpay/ipn | Callback có chữ ký; không cần session. IPN thành công đến sau khi đơn hủy/hết hạn: `RspCode 00`, đơn giữ `CANCELLED`, `paymentStatus=REFUND_REQUIRED`, mã giao dịch lưu ở `paymentReference`; IPN lặp lại trả `02` |
| GET | /orders/{id}/payment-status | Trạng thái gọn để FE poll: `orderStatus`, `paymentStatus`, `paymentReference`, `expiresAt`, `refundRequired`. `expiresAt` là mốc ISO-8601 (`createdAt` + 900 giây), `null` khi đơn không còn chờ thanh toán online |
| GET | /payments/vnpay/return | Đọc kết quả ở return URL, có kiểm tra chữ ký, không cần session (phiên có thể mất sau redirect). **Chỉ đọc, không đổi trạng thái đơn.** Trả `valid`, `orderId`, `responseCode`, `transactionStatus`, `amount` (VND, đã chia 100), `bankCode`, `transactionNo`, `orderStatus`, `paymentStatus`, `paymentReference`, `refundRequired`, `message`. Đủ để trang kết quả tự poll bằng chính endpoint này, không cần session. Sai chữ ký: `valid=false`, các trường còn lại `null`, không tiết lộ thông tin đơn |
| POST | /admin/orders/{id}/payment/reconcile | Đối soát `querydr` khi IPN không về. Trả `verified`, `responseCode`, `transactionStatus`, `message`, `applied` (`RspCode` khi có áp dụng, `null` khi không), `orderStatus`, `paymentStatus`, `paymentReference`, `refundRequired` |

`POST /orders` yêu cầu header `Idempotency-Key` (16–80 ký tự chữ/số/gạch ngang). Gửi lại cùng key và nội dung trả cùng đơn; cùng key nhưng nội dung khác bị từ chối. Không nhận giá/tổng tiền/quyền từ client.

Trạng thái thanh toán chỉ thay đổi qua IPN đã xác thực chữ ký hoặc qua đối soát `querydr` đã xác thực chữ ký phản hồi. Return URL là thông tin tham khảo cho giao diện, không phải nguồn sự thật. Đối soát chỉ áp dụng khi `vnp_ResponseCode=00` và `vnp_TransactionStatus` là `00` (đã thu tiền) hoặc `02` (thất bại); `01` (đang xử lý) được giữ nguyên để job quét lại hoặc hết hạn. Job hết hạn gọi `querydr` trước khi nhả tồn kho, tránh hủy đơn đã thu tiền.

Đơn trả thêm `refundRequired` và `paymentReference`. Admin lọc đơn cần hoàn tiền bằng `GET /admin/orders?refundRequired=true`; `GET /admin/dashboard` có `refundRequired` (tổng đơn cần hoàn tiền, không phụ thuộc khoảng ngày). Đơn `REFUND_REQUIRED` không có `allowedStatuses` và không lưu trữ được.

Bộ lọc: `brandIds=1&brandIds=2`, `spec.RAM=16%20GB&spec.RAM=32%20GB&spec.CPU=Core%20i7`. Cùng nhóm dùng OR, giữa các nhóm dùng AND; áp dụng ở database trước phân trang. `inStock=true` chỉ lấy tồn kho lớn hơn 0. Giá lọc là giá niêm yết. Facet và số lượng theo toàn bộ danh mục, chỉ tính sản phẩm công nghệ đang kinh doanh, không phụ thuộc các bộ lọc còn lại. Không chọn danh mục thì chỉ trả hãng/giá/tồn kho, tránh trộn thông số khác loại. `/brands` và `facets.brands` có `logoUrl`; `/categories` có `image` dùng làm icon.

Nếu có `cartItemIds`, chỉ mua các dòng được chọn và giữ các dòng còn lại trong giỏ. ID phải thuộc giỏ hiện tại, không trùng, tối đa 100 dòng. Bỏ trường này để mua toàn bộ giỏ.

Phân trang sản phẩm mặc định 12, tối đa 60. `sort`: newest, popular, price-asc, price-desc. `popular` xếp theo số lượng bán trong đơn CONFIRMED/SHIPPING/COMPLETED đã trả tiền hoặc COD (giống menu và `/home`), không dùng cột `products.sold`. Bộ lọc/sắp xếp giá dùng giá niêm yết; giá sau khuyến mãi có trong response. Page trả `content,totalElements,totalPages,number,first,last`.

## Quy tắc tiền (chờ xác nhận)

Quy tắc 1 và 2 dưới đây là hành vi hiện tại của `CommerceService`, được ghi lại bằng test tham số hóa trong `CommerceApiTests` (tên test nêu đúng quy tắc). Chưa có quyết định của chủ dự án nên **chưa đổi code**; nếu đổi quy tắc, sửa test tương ứng trước.

| # | Quy tắc hiện tại | Ví dụ | Trạng thái |
| --- | --- | --- | --- |
| 1 | Đơn tối thiểu của voucher so với **tổng toàn bộ các dòng được chọn**, không phải tổng các dòng đủ điều kiện. Mức giảm chỉ tính trên các dòng đủ điều kiện. | Voucher Laptop 10%, tối thiểu 5.000.000₫. Giỏ gồm laptop 1.000.000₫ và một món khác 4.000.000₫ vẫn được áp dụng, giảm 100.000₫. Món khác 3.999.999₫ thì bị từ chối | Chờ xác nhận |
| 2 | Miễn phí vận chuyển cho mọi đơn. `shippingFee` luôn trả `0`; trường vẫn giữ trên quote và đơn để đơn cũ (phí 30.000₫) báo đúng số đã thu. | Đơn 999.999₫ trả 0₫ phí vận chuyển | Chờ xác nhận |
| 3 | Lượt dùng voucher (`usedCount`) **được hoàn lại một lần** khi đơn bị hủy, thanh toán thất bại hoặc hết hạn thanh toán (đổi ngày 2026-09-30, theo `docs/PLAN-UI-REVIEW.md` L1). | Voucher giới hạn 1 lượt: khách A đặt rồi hủy, khách B dùng được | Đã đổi, cần chủ dự án xác nhận |

Đọc giỏ hàng, báo giá và danh sách/chi tiết đơn (`GET /cart`, `GET /cart/quote`, `GET /orders`, `GET /orders/{id}`) chạy `readOnly` và không dùng `SELECT ... FOR UPDATE`; `GET /cart` không tạo giỏ hàng mới. Báo giá chỉ để hiển thị: checkout khóa lại người mua, voucher và sản phẩm rồi kiểm tra từ đầu. `POST /cart/items`, `PATCH/DELETE /cart/items/{id}`, `POST /orders` và đổi trạng thái đơn vẫn khóa như trước.

## Quản trị — ADMIN bắt buộc

`GET /products/{id}` trả thêm `hasVariants` và `variants` đang bán: `id,sku,options,label,price,discountedPrice,quantity,active,imageUrl`. Cart và dòng đơn có `variantId,variantLabel`; dòng đơn giữ snapshot, không đổi theo chỉnh sửa sau đó.

Product admin nhận/trả `variants` (tối đa 100). Ví dụ:

```json
{"variants":[{"sku":"PHONE-BLACK-256","price":6000000,"quantity":10,"active":true,"imageUrl":"/uploads/example.png","options":{"Màu sắc":"Đen","Dung lượng":"256 GB"}}]}
```

PUT phải giữ `id` của phiên bản hiện có và gửi danh sách đầy đủ. Phiên bản bị bỏ khỏi danh sách được ngừng bán, không xóa lịch sử. Mỗi phiên bản có 1–6 thuộc tính, tên tối đa 80 ký tự, giá trị tối đa 100; tổ hợp và SKU không được trùng. Giá là số nguyên 0–1 tỷ VND, kho 0–1 triệu. Giá/kho sản phẩm cha được tính lại từ phiên bản đang bán. Sản phẩm có phiên bản không nhận PUT thiếu trường `variants`. Chuyển sản phẩm thường sang có phiên bản bị chặn nếu còn đơn thường đang xử lý.

Upload trả URL; gửi URL đó trong `images` hoặc `variants[].imageUrl` khi lưu sản phẩm. Thứ tự `images` được lưu; ảnh đầu là ảnh đại diện.

`GET /admin/{resource}?page=0&size=20&search=...`, `POST /admin/{resource}`, `PUT /admin/{resource}/{id}`, `DELETE /admin/{resource}/{id}`. Resource: products,categories,brands,banners,vouchers,promotions,users,reviews. Policies có controller riêng trả danh sách và hỗ trợ đổi active.

`GET /admin/{resource}/{id}` trả dữ liệu chi tiết để chỉnh sửa, `404` nếu không tồn tại. Tạo mới thành công trả `201` và header `Location` trỏ đến tài nguyên đó; xóa/ngừng bán trả `204`. Users được tạo qua đăng ký, reviews được tạo bởi khách đủ điều kiện, không hỗ trợ admin POST cho hai loại này. DTO người dùng không trả mật khẩu/token. Vue đọc lại chi tiết theo ID khi mở form sửa.

Toàn bộ nghiệp vụ đi qua `/api/v1`; route giao diện trả Vue `index.html`. Endpoint MVC cũ như `/admin/user/delete/{id}` không còn hoạt động. Lỗi thiếu tài nguyên, sai phương thức và sai Content-Type trả JSON với mã `404`, `405`, `415`; kiểm tra xác thực/quyền/CSRF vẫn áp dụng trước controller. Link email vẫn mở `/verify?code=...` trên Vue, sau đó frontend gửi POST xác minh. GET trực tiếp `/api/v1/auth/verify` không thay đổi tài khoản. Callback GET VNPay là ngoại lệ theo giao thức của nhà cung cấp, có kiểm tra chữ ký.

- Product: name,sku,slug,price,quantity,warrantyMonths,categoryId,brandId,shortDesc,detailDesc,images (list URL),specs (list name/value),active,technologyCatalog. Xóa sản phẩm là ngừng bán.
- Category: name,description,image,brandIds. Product phải chọn brand thuộc category đã gán; không bỏ quan hệ đang được sản phẩm sử dụng.
- Promotion: name,description,discountRate,startDate/endDate (ISO instant),scopeType (ALL, CATEGORY, BRAND, PRODUCT),categoryIds,productIds,brandIds,active. Phản hồi có thêm `scopeDescription`. `POST/PUT /admin/promotions` trả `{id, warnings: [...]}`; `warnings` liệt kê khuyến mãi đang bật trùng phạm vi và thời gian (không chặn lưu). Danh sách admin: sản phẩm có `categoryName`, `brandName`, `discountRate`, `promotionName`; danh mục và thương hiệu có `productCount` (tất cả sản phẩm, kể cả ngừng bán).
- Voucher: code,discount (%),scopeType,categoryIds,brandIds,productIds,active,startDate/endDate (ISO instant hoặc null),minimumOrder (VND),maximumDiscount (VND hoặc null),usageLimit (số nguyên dương hoặc null). `usedCount` chỉ đọc, không nhận từ client. Khóa voucher trong transaction; đơn thành công tiêu thụ một lượt, retry không tăng lượt, hủy đơn, thanh toán thất bại hoặc hết hạn hoàn lại một lượt. Minimum áp dụng trên subtotal sau khuyến mãi; giảm phần trăm trên các dòng thuộc scope, tối đa maximumDiscount nếu có. `all` còn được trả để tương thích client cũ.
- Scope chung: ALL/CATEGORY/BRAND/PRODUCT; non-ALL phải có ID tồn tại, OR trong cùng danh sách, xóa danh sách không tương ứng. Chi tiết và ví dụ: [CATEGORY-BRAND-SCOPES.md](CATEGORY-BRAND-SCOPES.md).
- `GET /admin/options/{resource}`: brands/categories/products, tìm `search`, phân trang `page` (20 mục), `ids` nạp lại nhãn. categories và brands trả thêm `image` và `productCount` (số sản phẩm đang bán); products lọc theo `categoryId` hoặc `brandId` và trả `categoryId`, `brandId`, `active`; brands còn nhận `categoryId`/`categoryIds` để giới hạn hãng được gán. Chỉ ADMIN.
- `/catalog/facets` thêm minWarranty/maxWarranty; `/products` nhận minWarranty/maxWarranty (tháng), promotionId lọc theo đủ bốn scope; `/offers` trả scopeType/scopeNames.
- User: chỉ cập nhật role,enabled,locked; không đổi chính tài khoản đang dùng. Chỉ xóa được tài khoản chưa có đơn hoặc ràng buộc liên quan.
- Review: danh sách/xóa; không tạo/sửa đánh giá thay khách.
- Upload: `POST /admin/uploads`, multipart field `file`, tối đa 5 MB, PNG/JPEG/WebP; tên tệp do server tạo.
- `GET /admin/orders` hỗ trợ search/status/page/size; `GET /admin/orders/{id}` xem chi tiết.
- `PATCH /admin/orders/{id}/status`: PENDING → CONFIRMED → SHIPPING → COMPLETED; PENDING/CONFIRMED có thể CANCELLED nếu chưa trả tiền. Đơn online phải PAID trước xác nhận/giao.
- `GET /admin/orders/{id}/invoice`: PDF, tên sản phẩm từ snapshot.
- `GET /admin/dashboard?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`: số liệu, doanh thu theo ngày, sản phẩm bán chạy; mặc định 30 ngày.
- `GET /admin/reports/orders`: CSV đối soát đơn hàng.
