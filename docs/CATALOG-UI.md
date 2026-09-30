# Danh mục, logo hãng và bộ lọc

## Quản lý icon và logo

- Mở **Quản trị → Danh mục → Sửa**. Tải PNG/JPEG/WebP (tối đa 5 MB), xem trước rồi bấm **Lưu thay đổi**. Ảnh này dùng làm icon trong menu và danh mục trang chủ. Danh mục chưa có ảnh dùng hình minh họa mặc định.
- Mở **Quản trị → Thương hiệu → Sửa**, tải logo và lưu tương tự. Logo xuất hiện ở bảng hãng trong menu và bộ lọc sản phẩm. Khi chưa có logo hoặc ảnh không tải được, hiển thị tên hãng.
- Ảnh lưu trên máy chủ trong `data/uploads`; đường dẫn gắn với bản ghi MySQL. Không cần đổi source code để thay ảnh. Có thể nhập đường dẫn ảnh sẵn có thay vì tải tệp.
- Menu công khai chỉ hiện danh mục/hãng có sản phẩm công nghệ đang kinh doanh. Tạo sản phẩm và gắn danh mục/hãng để hiển thị mục mới.

## Bộ lọc riêng theo danh mục

Trong **Quản trị → Sản phẩm → Sửa**, nhập mục **Thông số, mỗi dòng: Tên: Giá trị**, ví dụ:

```text
RAM: 16 GB
CPU: Core i7
Tần số quét: 144 Hz
Tính năng đặc biệt: Mỏng nhẹ
```

Chỉ nhập thông số đúng với sản phẩm. Dùng tên/đơn vị nhất quán giữa các sản phẩm, chẳng hạn luôn dùng `16 GB`, tránh trộn `16GB` với `16 GB`. Khi lưu, các nhóm và giá trị tự xuất hiện trong bộ lọc của danh mục đó. Đây là thông số sản phẩm; tùy chọn phiên bản mua hàng tiếp tục được quản lý riêng.

- Nhiều giá trị trong cùng nhóm: chấp nhận **một trong các giá trị**.
- Giữa các nhóm/hãng/giá/tồn kho: sản phẩm phải đáp ứng **đồng thời**.
- Số lượng trong ngoặc vuông tính theo toàn bộ danh mục, không đổi theo các lựa chọn khác; sản phẩm ngừng bán không được tính.
- Giá lọc/sắp xếp dùng giá niêm yết; thẻ vẫn hiển thị giá khuyến mãi.
- URL lưu bộ lọc; có thể chia sẻ, tải lại và dùng nút quay lại của trình duyệt. Chuyển danh mục xóa bộ lọc cũ để tránh thông số không liên quan.
- Điện thoại: bấm **Mở bộ lọc**, chọn như trên desktop rồi **Thu gọn** để xem sản phẩm.

## Trạng thái bán và tồn kho

Trong quản trị sản phẩm, **Ngừng bán** ẩn sản phẩm khỏi cửa hàng; dòng đã ngừng có nút **Bán lại**. Bán lại giữ nguyên tồn kho và trạng thái từng phiên bản. Sản phẩm tồn 0 vẫn có thể mở bán lại nhưng được ghi rõ hết hàng và khách chưa thể mua; cần nhập thêm tồn kho/bật phiên bản phù hợp. Nếu đang lọc sản phẩm ngừng kinh doanh, dòng vừa bán lại sẽ rời danh sách lọc.

Hàng còn tồn luôn đứng trước hàng hết tồn ở trang chủ, danh mục, sản phẩm liên quan và danh sách quản trị, kể cả khi phân trang. Sắp xếp giá/bán chạy/mới nhất áp dụng trong từng nhóm này. Giỏ hàng không tự chọn và không cho thanh toán dòng đã ngừng bán, hết hàng hoặc số lượng vượt tồn kho; những dòng đủ điều kiện khác vẫn mua bình thường.

## Sử dụng menu

Bấm **Danh mục sản phẩm**, rê chuột vào danh mục hoặc bấm mũi tên bên cạnh để xem hãng, giá và thông số. Bấm tên danh mục để xem tất cả sản phẩm. Bấm lựa chọn trong bảng để đi thẳng đến kết quả đã lọc. Bấm ngoài bảng, nút đóng hoặc Escape để đóng menu.

Frontend: `CategoryMenu.vue`, `BrandMark.vue`, `CatalogPage.vue`, `catalog.css`. API: `GET /api/v1/catalog/facets` và bộ lọc trên `GET /api/v1/products`. Các trường ảnh đã có trong schema; không cần migration hay xóa dữ liệu.
