import { ref } from "vue";
export const discountPercent = (price, discountedPrice) => {
  if (!(price > 0) || !(discountedPrice < price)) return "0";
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(
    Math.min(100, Math.max(0, ((price - discountedPrice) / price) * 100)),
  );
};
/** The percent off a sale tag shows: the running promotion's own rate, else worked out from the two prices. */
export const saleRate = (product) => {
  if (!product) return 0;
  if (product.discountRate > 0) return product.discountRate;
  return Number(String(discountPercent(product.price, product.discountedPrice)).replace(",", ".")) || 0;
};
export const rateText = (rate) =>
  new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(rate);
export const money = (value) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    value || 0,
  );
export const date = (value) =>
  value ? new Date(value).toLocaleString("vi-VN") : "—";
export const statuses = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  SHIPPING: "Đang giao",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};
export const paymentStatuses = {
  UNPAID: "Chưa thanh toán",
  PAID: "Đã thanh toán",
  FAILED: "Thanh toán thất bại",
  EXPIRED: "Hết hạn thanh toán",
  REFUND_REQUIRED: "Cần hoàn tiền",
};
export const vnpayResponses = {
  "00": "Giao dịch thành công",
  "07": "Trừ tiền thành công. Giao dịch bị nghi ngờ liên quan tới lừa đảo hoặc bất thường",
  "09": "Thẻ hoặc tài khoản chưa đăng ký dịch vụ Internet Banking tại ngân hàng",
  "10": "Khách hàng xác thực thông tin thẻ hoặc tài khoản không đúng quá 3 lần",
  "11": "Đã hết hạn chờ thanh toán, vui lòng thực hiện lại giao dịch",
  "12": "Thẻ hoặc tài khoản của khách hàng bị khóa",
  "13": "Khách hàng nhập sai mật khẩu xác thực giao dịch OTP",
  "24": "Khách hàng hủy giao dịch",
  "51": "Tài khoản không đủ số dư",
  "65": "Tài khoản đã vượt quá hạn mức giao dịch trong ngày",
  "75": "Ngân hàng thanh toán đang bảo trì",
  "79": "Khách hàng nhập sai mật khẩu thanh toán quá số lần quy định",
  "99": "Lỗi khác",
};
export function imageUrl(value) {
  return !value
    ? "/tech/laptop.svg"
    : value.startsWith("/") || value.startsWith("https://")
      ? value
      : `/images/${value}`;
}
export function imageError(event) {
  if (!event.target.dataset.fallback) {
    event.target.dataset.fallback = "true";
    event.target.src = "/tech/laptop.svg";
  }
}
export function useTask() {
  const busy = ref(false),
    error = ref(""),
    message = ref("");
  async function run(action) {
    busy.value = true;
    error.value = "";
    message.value = "";
    try {
      return await action();
    } catch (e) {
      error.value = e.message;
      return undefined;
    } finally {
      busy.value = false;
    }
  }
  return { busy, error, message, run };
}
