export const resources = {
  products: {
    title: "Sản phẩm",
    fields: [
      ["name", "Tên sản phẩm"],
      ["sku", "SKU"],
      ["slug", "Slug"],
      ["price", "Giá niêm yết", "number"],
      ["quantity", "Tồn kho", "number"],
      ["warrantyMonths", "Bảo hành (tháng)", "number"],
      ["categoryId", "Danh mục", "category"],
      ["brandId", "Thương hiệu", "brand"],
      ["shortDesc", "Mô tả ngắn", "textarea"],
      ["detailDesc", "Mô tả chi tiết", "textarea"],
      ["images", "Đường dẫn ảnh, mỗi dòng một ảnh", "lines"],
      ["specs", "Thông số, mỗi dòng: Tên: Giá trị", "specs"],
      ["technologyCatalog", "Hiển thị trong cửa hàng công nghệ", "checkbox"],
      ["active", "Đang kinh doanh", "checkbox"],
    ],
    sections: [
      { title: "Thông tin cơ bản", keys: ["name", "sku", "slug", "categoryId", "brandId"], notice: "brands" },
      { title: "Giá và kho", hint: "Nếu sản phẩm có phiên bản, giá và tồn kho chung tính từ các phiên bản.", keys: ["price", "quantity", "warrantyMonths"] },
      { title: "Mô tả", keys: ["shortDesc", "detailDesc"], wide: true },
      { title: "Hình ảnh", hint: "Ảnh đầu tiên là ảnh đại diện. Nhập đường dẫn, mỗi dòng một ảnh, hoặc tải ảnh lên.", keys: ["images"], media: true, wide: true },
      { title: "Thông số kỹ thuật", hint: "Thông số của các sản phẩm tạo thành bộ lọc theo danh mục. Nhập cùng tên thống nhất, ví dụ RAM: 16 GB hoặc Tần số quét: 144 Hz.", keys: ["specs"], wide: true },
      { title: "Hiển thị", keys: ["technologyCatalog", "active"], wide: true },
    ],
  },
  categories: {
    title: "Danh mục",
    fields: [
      ["name", "Tên danh mục"],
      ["description", "Mô tả", "textarea"],
      ["image", "Icon danh mục (đường dẫn ảnh hoặc tải ảnh bên dưới)"],
    ],
    sections: [
      { title: "Thông tin", keys: ["name", "description"], wide: true },
      { title: "Icon danh mục", hint: "Icon hiển thị ở menu danh mục và trang chủ. Nên dùng ảnh vuông, nền trong suốt.", keys: ["image"], media: true, wide: true },
      { title: "Thương hiệu áp dụng", hint: "Sản phẩm của danh mục chỉ được chọn các hãng được gán ở đây.", keys: [], brandPicker: true },
    ],
  },
  brands: {
    title: "Thương hiệu",
    fields: [
      ["name", "Tên thương hiệu"],
      ["description", "Mô tả", "textarea"],
      ["logoUrl", "Đường dẫn logo"],
    ],
    sections: [
      { title: "Thông tin", keys: ["name", "description"], wide: true },
      { title: "Logo", hint: "Logo hiển thị trong menu và bộ lọc hãng. Nên dùng ảnh nền trong suốt.", keys: ["logoUrl"], media: true, wide: true },
    ],
  },
  banners: {
    title: "Banner",
    fields: [
      ["name", "Tên banner"],
      ["imageUrl", "Đường dẫn ảnh"],
      ["link", "Liên kết"],
      ["placement", "Vị trí", "bannerPlacement"],
      ["technologyCatalog", "Hiển thị trong cửa hàng công nghệ", "checkbox"],
      ["active", "Hiển thị", "checkbox"],
    ],
    sections: [
      { title: "Thông tin", keys: ["name", "link", "placement"], wide: true },
      { title: "Ảnh banner", hint: "Ảnh ngang, hiển thị ở trang chủ.", keys: ["imageUrl"], media: true, wide: true },
      { title: "Hiển thị", keys: ["technologyCatalog", "active"], wide: true },
    ],
  },
  vouchers: {
    title: "Voucher",
    fields: [
      ["code", "Mã giảm giá"],
      ["discount", "Giảm giá (%)", "number"],
      ["active", "Kích hoạt", "checkbox"],
      ["startDate", "Bắt đầu (để trống nếu không giới hạn)", "datetime-local"],
      ["endDate", "Kết thúc (để trống nếu không giới hạn)", "datetime-local"],
      ["minimumOrder", "Giá trị đơn tối thiểu (VND)", "number"],
      ["maximumDiscount", "Giảm tối đa (VND, để trống nếu không giới hạn)", "number"],
      ["usageLimit", "Số lượt tối đa (để trống nếu không giới hạn)", "number"],
    ],
  },
  promotions: {
    title: "Khuyến mãi",
    fields: [
      ["name", "Tên chương trình"],
      ["description", "Mô tả", "textarea"],
      ["discountRate", "Giảm giá (%)", "number"],
      ["startDate", "Bắt đầu", "datetime-local"],
      ["endDate", "Kết thúc", "datetime-local"],
      ["active", "Kích hoạt", "checkbox"],
    ],
  },
  policies: {
    title: "Chính sách",
    fields: [
      ["name", "Tên chính sách"],
      ["content", "Nội dung", "textarea"],
      ["technologyCatalog", "Hiển thị trong cửa hàng công nghệ", "checkbox"],
      ["active", "Hiển thị", "checkbox"],
    ],
    sections: [
      { title: "Nội dung", keys: ["name", "content"], wide: true },
      { title: "Hiển thị", keys: ["technologyCatalog", "active"], wide: true },
    ],
  },
  users: {
    title: "Người dùng",
    fields: [
      ["role", "Quyền", "role"],
      ["enabled", "Đã kích hoạt", "checkbox"],
      ["locked", "Khóa tài khoản", "checkbox"],
    ],
    sections: [{ title: "Quyền và trạng thái", keys: ["role", "enabled", "locked"], wide: true }],
    noCreate: true,
  },
  reviews: { title: "Đánh giá", fields: [], noCreate: true, noEdit: true },
  orders: { title: "Đơn hàng", fields: [], noCreate: true, noEdit: true },
};
