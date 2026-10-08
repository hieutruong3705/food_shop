<script setup>
import { computed, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import TaskState from "../components/TaskState.vue";
import { date, money, paymentStatuses, statuses, useTask } from "../lib/shop";
import { api } from "../services/api";

const route = useRoute();
const orders = ref([]);
const order = ref(null);
const paymentState = ref(null);
const payBusy = ref(false);
const now = ref(Date.now());
const { busy, error, message, run } = useTask();
let countdownTimer;

const effectivePaymentStatus = computed(
  () => paymentState.value?.paymentStatus || order.value?.paymentStatus || "",
);
const effectivePaymentReference = computed(
  () => paymentState.value?.paymentReference || order.value?.paymentReference || "",
);
const refundRequired = computed(
  () => Boolean(paymentState.value?.refundRequired || order.value?.refundRequired),
);
const expiresAt = computed(() => paymentState.value?.expiresAt || order.value?.expiresAt || "");
const expiresTime = computed(() => {
  if (!expiresAt.value) return 0;
  const value = new Date(expiresAt.value).getTime();
  return Number.isNaN(value) ? 0 : value;
});
const paymentExpired = computed(
  () => effectivePaymentStatus.value === "EXPIRED" || (expiresTime.value > 0 && now.value >= expiresTime.value),
);
const paymentCountdown = computed(() => {
  if (!expiresTime.value || paymentExpired.value) return "";
  const seconds = Math.max(0, Math.ceil((expiresTime.value - now.value) / 1000));
  const minutes = Math.floor(seconds / 60);
  return `${minutes}:${String(seconds % 60).padStart(2, "0")}`;
});
const canPay = computed(
  () =>
    order.value?.status === "PENDING" &&
    order.value?.paymentMethod === "VNPAY" &&
    effectivePaymentStatus.value === "UNPAID" &&
    !paymentExpired.value,
);

function clearCountdown() {
  if (countdownTimer) clearInterval(countdownTimer);
  countdownTimer = undefined;
}

function startCountdown() {
  clearCountdown();
  if (!expiresTime.value) return;
  now.value = Date.now();
  countdownTimer = setInterval(() => {
    now.value = Date.now();
    if (paymentExpired.value) clearCountdown();
  }, 1000);
}

async function loadPaymentStatus() {
  if (
    !order.value ||
    order.value.paymentMethod !== "VNPAY" ||
    !["PENDING", "CANCELLED"].includes(order.value.status)
  )
    return;
  paymentState.value = await api(`/orders/${order.value.id}/payment-status`);
  order.value = {
    ...order.value,
    status: paymentState.value.orderStatus || order.value.status,
    paymentStatus: paymentState.value.paymentStatus || order.value.paymentStatus,
    paymentReference: paymentState.value.paymentReference || order.value.paymentReference,
    refundRequired: paymentState.value.refundRequired,
  };
  startCountdown();
}

watch(
  () => route.params.id,
  () =>
    run(async () => {
      order.value = null;
      paymentState.value = null;
      clearCountdown();
      if (route.params.id) {
        order.value = await api(`/orders/${route.params.id}`);
        await loadPaymentStatus();
      } else {
        orders.value = await api("/orders");
      }
    }),
  { immediate: true },
);

function pay() {
  if (!canPay.value || payBusy.value) return;
  run(async () => {
    payBusy.value = true;
    try {
      const result = await api(`/orders/${order.value.id}/payment`, {
        method: "POST",
      });
      const url = new URL(result.url);
      if (url.origin !== "https://sandbox.vnpayment.vn")
        throw new Error("Địa chỉ thanh toán không hợp lệ");
      window.location.assign(url.href);
    } finally {
      payBusy.value = false;
    }
  });
}

function cancel() {
  if (!window.confirm("Bạn muốn hủy đơn hàng này?")) return;
  run(async () => {
    order.value = await api(`/orders/${order.value.id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: "CANCELLED" }),
    });
    await loadPaymentStatus();
    message.value = "Đã hủy đơn hàng.";
  });
}

onUnmounted(clearCountdown);
</script>

<template>
  <div class="page-heading">
    <span class="eyebrow">TÀI KHOẢN / ĐƠN HÀNG</span>
    <h1>{{ route.params.id ? `Đơn hàng #${route.params.id}` : "Đơn hàng của tôi" }}</h1>
  </div>
  <TaskState :busy="busy" :error="error" :message="message" />

  <template v-if="order">
    <div class="two-column">
      <section class="panel">
        <span class="status">{{ statuses[order.status] || order.status }}</span>
        <p>Ngày đặt: {{ date(order.createdAt) }}</p>
        <h2>Thông tin nhận hàng</h2>
        <p>{{ order.receiverName }} · {{ order.receiverPhone }}</p>
        <p>{{ order.receiverAddress }}</p>
        <p v-if="order.note">Ghi chú: {{ order.note }}</p>
        <p>
          {{ order.paymentMethod }} ·
          {{ paymentStatuses[effectivePaymentStatus] || effectivePaymentStatus }}
        </p>
        <p v-if="refundRequired" class="muted">
          Thanh toán đến sau khi đơn đã hủy. Cửa hàng sẽ hoàn tiền cho bạn, mã giao dịch
          {{ effectivePaymentReference || "—" }}.
        </p>
        <p v-if="canPay && paymentCountdown" class="muted">
          Còn {{ paymentCountdown }} để thanh toán.
        </p>
        <p
          v-else-if="
            order.paymentMethod === 'VNPAY' &&
            order.status === 'PENDING' &&
            effectivePaymentStatus === 'UNPAID' &&
            paymentExpired
          "
          class="muted"
        >
          Phiên thanh toán đã hết hạn.
        </p>
        <button v-if="canPay" :disabled="busy || payBusy" @click="pay">
          Thanh toán qua VNPay
        </button>
        <button
          v-if="order.status === 'PENDING' && effectivePaymentStatus !== 'PAID'"
          :disabled="busy || payBusy"
          class="danger"
          @click="cancel"
        >
          Hủy đơn hàng
        </button>
      </section>
      <section class="panel">
        <h2>Chi tiết thanh toán</h2>
        <div v-for="item in order.items" :key="item.id" class="summary-row">
          <RouterLink :to="`/products/${item.productId}`">
            {{ item.name }}{{ item.variantLabel ? " · " + item.variantLabel : "" }} × {{ item.quantity }}
          </RouterLink>
          <strong>{{ money(item.price * item.quantity) }}</strong>
        </div>
        <div v-if="order.shippingFee" class="summary-row">
          <span>Phí giao hàng</span><span>{{ money(order.shippingFee) }}</span>
        </div>
        <div class="summary-row">
          <span>Giảm giá {{ order.voucherCode }}</span><span>−{{ money(order.discountAmount) }}</span>
        </div>
        <div class="summary-row total">
          <strong>Tổng thanh toán</strong><strong>{{ money(order.totalPrice) }}</strong>
        </div>
      </section>
    </div>
    <p><RouterLink to="/orders">← Tất cả đơn hàng</RouterLink></p>
  </template>

  <template v-else-if="!route.params.id">
    <div v-if="orders.length" class="panel table-wrap">
      <table>
        <thead>
          <tr>
            <th>Mã đơn</th>
            <th>Ngày đặt</th>
            <th>Trạng thái</th>
            <th>Tổng tiền</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in orders" :key="o.id">
            <td>#{{ o.id }}</td>
            <td>{{ date(o.createdAt) }}</td>
            <td>{{ statuses[o.status] || o.status }}</td>
            <td>{{ money(o.totalPrice) }}</td>
            <td><RouterLink :to="`/orders/${o.id}`">Chi tiết →</RouterLink></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else-if="!busy && !error" class="empty">
      <p>Bạn chưa có đơn hàng nào.</p>
      <RouterLink to="/products" class="button">Khám phá sản phẩm</RouterLink>
    </div>
  </template>
</template>
