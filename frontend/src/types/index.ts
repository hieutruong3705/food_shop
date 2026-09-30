export interface User {
  id: number;
  email: string;
  fullName: string;
  role: string;
  enabled: boolean;
}

export interface Profile {
  fullName: string;
  phone: string;
  address: string;
}

export interface ProductSummary {
  id: number;
  name: string;
  price: number;
  discountedPrice?: number;
  image: string;
  shortDescription?: string;
  quantity: number;
  active: boolean;
  category: Category;
  brand: Brand;
}

export interface Variant {
  id: number;
  sku: string;
  options: Record<string, string>;
  label: string;
  price: number;
  discountedPrice?: number;
  quantity: number;
  active: boolean;
  imageUrl?: string;
}

export interface ProductDetail {
  product: ProductSummary;
  detailDescription?: string;
  images: string[];
  specifications: Array<{ name: string; value: string }>;
  sku: string;
  slug: string;
  warrantyMonths?: number;
  categoryId: number;
  variants: Variant[];
  hasVariants: boolean;
}

export interface CartLine {
  id: number;
  productId: number;
  name: string;
  image: string;
  quantity: number;
  stock: number;
  price: number;
  active: boolean;
  variantId?: number;
  variantLabel?: string;
}

export interface CartView {
  items: CartLine[];
  count: number;
  total: number;
}

export interface Quote {
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  totalPrice: number;
  voucherCode?: string;
}

export interface OrderLine {
  id: number;
  productId: number;
  name: string;
  sku: string;
  quantity: number;
  price: number;
  variantId?: number;
  variantLabel?: string;
}

export interface OrderView {
  id: number;
  receiverName: string;
  receiverAddress: string;
  receiverPhone: string;
  status: string;
  paymentMethod: string;
  paymentStatus: string;
  createdAt: string;
  totalPrice: number;
  shippingFee: number;
  discountAmount: number;
  voucherCode?: string;
  items: OrderLine[];
  archived: boolean;
  note?: string;
}

export interface ReviewView {
  id: number;
  rating: number;
  content: string;
  author: string;
  productId: number;
  productName: string;
  createdAt: string;
  editable: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface Category {
  id: number;
  name: string;
  image?: string;
  productCount: number;
}

export interface Brand {
  id: number;
  name: string;
}

export interface Banner {
  id: number;
  name: string;
  imageUrl: string;
  link: string;
}

export type Row = Record<string, any>;
