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
  },
  categories: {
    title: "Danh mục",
    fields: [
      ["name", "Tên danh mục"],
      ["description", "Mô tả", "textarea"],
      ["image", "Icon danh mục (đường dẫn ảnh hoặc tải ảnh bên dưới)"],
    ],
  },
  brands: {
    title: "Thương hiệu",
    fields: [
      ["name", "Tên thương hiệu"],
      ["description", "Mô tả", "textarea"],
      ["logoUrl", "Đường dẫn logo"],
    ],
  },
  banners: {
    title: "Banner",
    fields: [
      ["name", "Tên banner"],
      ["imageUrl", "Đường dẫn ảnh"],
      ["link", "Liên kết"],
      ["technologyCatalog", "Hiển thị trong cửa hàng công nghệ", "checkbox"],
      ["active", "Hiển thị", "checkbox"],
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
  },
  users: {
    title: "Người dùng",
    fields: [
      ["role", "Quyền", "role"],
      ["enabled", "Đã kích hoạt", "checkbox"],
      ["locked", "Khóa tài khoản", "checkbox"],
    ],
    noCreate: true,
  },
  reviews: { title: "Đánh giá", fields: [], noCreate: true, noEdit: true },
  orders: { title: "Đơn hàng", fields: [], noCreate: true, noEdit: true },
};
