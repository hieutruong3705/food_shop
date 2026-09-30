import { ref } from "vue";
export const discountPercent = (price, discountedPrice) => {
  if (!(price > 0) || !(discountedPrice < price)) return "0";
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(
    Math.min(100, Math.max(0, ((price - discountedPrice) / price) * 100)),
  );
};
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
