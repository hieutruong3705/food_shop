<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute } from "vue-router";
import TaskState from "../components/TaskState.vue";
import { money, paymentStatuses, statuses, useTask, vnpayResponses } from "../lib/shop";
import { api } from "../services/api";

const route = useRoute();
const result = ref(null);
const paymentState = ref(null);
const polling = ref(false);
const timedOut = ref(false);
const pollError = ref("");
const { busy, error, run } = useTask();
let pollTimer;
let pollStartedAt = 0;

const finalStatuses = ["PAID", "FAILED", "EXPIRED"];
const currentPaymentStatus = computed(
  () => paymentState.value?.paymentStatus || result.value?.paymentStatus || "",
);
const currentOrderStatus = computed(
  () => paymentState.value?.orderStatus || result.value?.orderStatus || "",
);
const paymentReference = computed(
  () => paymentState.value?.paymentReference || result.value?.paymentReference || result.value?.transactionNo || "",
);
const refundRequired = computed(
  () => Boolean(paymentState.value?.refundRequired || result.value?.refundRequired),
);
const responseDescription = computed(() => {
  const code = result.value?.responseCode;
  return vnpayResponses[code] || code || "Không rõ";
});
const vnpayReportedSuccess = computed(
  () => result.value?.responseCode === "00" && result.value?.transactionStatus === "00",
);
const isFinal = computed(() => finalStatuses.includes(currentPaymentStatus.value));
const isPaid = computed(() => currentPaymentStatus.value === "PAID");
const isFailed = computed(() => ["FAILED", "EXPIRED"].includes(currentPaymentStatus.value));

function queryString() {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(route.query)) {
    if (Array.isArray(value)) value.forEach((entry) => params.append(key, entry ?? ""));
    else if (value != null) params.append(key, value);
  }
  return params.toString();
}

function stopPolling() {
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = undefined;
  polling.value = false;
}

// The signed return callback carries the order state too, so the page keeps working
// for a payer whose session did not survive the redirect back from VNPay.
async function readReturn() {
  const qs = queryString();
  return api(`/payments/vnpay/return${qs ? `?${qs}` : ""}`);
}

function applyResult(data) {
  result.value = data;
  paymentState.value = data?.valid
    ? {
        orderStatus: data.orderStatus,
        paymentStatus: data.paymentStatus,
        paymentReference: data.paymentReference || data.transactionNo,
        refundRequired: data.refundRequired,
      }
    : null;
}

async function pollStatus() {
  if (!result.value?.orderId) return stopPolling();
  try {
    applyResult(await readReturn());
    pollError.value = "";
    if (finalStatuses.includes(currentPaymentStatus.value)) return stopPolling();
    if (Date.now() - pollStartedAt >= 60000) {
      timedOut.value = true;
      stopPolling();
    }
  } catch (e) {
    pollError.value = e.message;
    if (Date.now() - pollStartedAt >= 60000) {
      timedOut.value = true;
      stopPolling();
    }
  }
}

function startPolling() {
  stopPolling();
  timedOut.value = false;
  pollError.value = "";
  pollStartedAt = Date.now();
  polling.value = true;
  // The caller has just read the callback, so the first interval tick is the next useful one.
  pollTimer = setInterval(pollStatus, 3000);
}

function maybePoll() {
  if (result.value?.valid && !isFinal.value) startPolling();
}

function checkAgain() {
  run(async () => {
    if (!result.value?.orderId) return;
    applyResult(await readReturn());
    if (!finalStatuses.includes(currentPaymentStatus.value)) startPolling();
  });
}

onMounted(() =>
  run(async () => {
    applyResult(await readReturn());
    maybePoll();
  }),
);
onUnmounted(stopPolling);
</script>

<template>
  <div class="page-heading">
    <span class="eyebrow">THANH TOÁN VNPAY</span>
    <h1>Kết quả thanh toán</h1>
    <p>Đơn hàng chỉ được xác nhận khi máy chủ nhận được kết quả thanh toán chính thức từ VNPay.</p>
  </div>
  <TaskState :busy="busy" :error="error || pollError" />

  <section v-if="result && !result.valid" class="empty">
    <h2>Không xác thực được kết quả thanh toán</h2>
    <p>Vui lòng kiểm tra lại danh sách đơn hàng hoặc liên hệ cửa hàng nếu bạn đã thao tác thanh toán.</p>
    <RouterLink class="button" to="/orders">Về đơn hàng</RouterLink>
  </section>

  <div v-else-if="result?.valid" class="payment-return">
    <section class="panel payment-result">
      <span class="status" :class="{ success: vnpayReportedSuccess, danger: !vnpayReportedSuccess }">
        {{ vnpayReportedSuccess ? "VNPay báo giao dịch thành công" : "VNPay báo giao dịch chưa thành công" }}
      </span>
      <h2>Thông tin từ VNPay</h2>
      <table class="result-table">
        <tbody>
          <tr>
            <th>Số tiền</th>
            <td>{{ money(result.amount) }}</td>
          </tr>
          <tr>
            <th>Ngân hàng</th>
            <td>{{ result.bankCode || "—" }}</td>
          </tr>
          <tr>
            <th>Mã giao dịch</th>
            <td>{{ result.transactionNo || "—" }}</td>
          </tr>
          <tr>
            <th>Mã phản hồi</th>
            <td>{{ result.responseCode || "—" }} · {{ responseDescription }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel payment-confirmation">
      <p v-if="refundRequired" class="notice error">
        Thanh toán đến sau khi đơn đã hủy, cửa hàng sẽ hoàn tiền. Mã giao dịch:
        {{ paymentReference || "—" }}.
      </p>

      <template v-if="isPaid">
        <span class="status success">Đã xác nhận thanh toán</span>
        <h2>Thanh toán đã được xác nhận</h2>
        <p>Đơn hàng #{{ result.orderId }} đã được ghi nhận thanh toán thành công.</p>
        <RouterLink class="button" :to="`/orders/${result.orderId}`">Xem đơn</RouterLink>
      </template>

      <template v-else-if="isFailed">
        <span class="status danger">{{ paymentStatuses[currentPaymentStatus] || currentPaymentStatus }}</span>
        <h2>Thanh toán không hoàn tất</h2>
        <p>
          Trạng thái đơn:
          {{ statuses[currentOrderStatus] || currentOrderStatus || "Đang cập nhật" }}.
        </p>
        <RouterLink class="button" to="/checkout">Đặt lại</RouterLink>
      </template>

      <template v-else-if="timedOut">
        <span class="status">Đang chờ cập nhật</span>
        <h2>Đơn sẽ được cập nhật sau</h2>
        <p>Máy chủ chưa nhận được xác nhận cuối cùng từ VNPay. Bạn có thể kiểm tra lại sau ít phút.</p>
        <button :disabled="busy" @click="checkAgain">Kiểm tra lại</button>
        <RouterLink class="button secondary" :to="`/orders/${result.orderId}`">Xem đơn</RouterLink>
      </template>

      <template v-else>
        <span class="status">Chờ IPN từ VNPay</span>
        <h2><span class="payment-spinner" aria-hidden="true"></span>Đang chờ xác nhận từ VNPay…</h2>
        <p>
          VNPay đã trả kết quả về trình duyệt, nhưng trạng thái thanh toán sẽ được chốt bởi xác nhận
          server-to-server.
        </p>
      </template>

      <p class="muted">
        Thanh toán:
        {{ paymentStatuses[currentPaymentStatus] || currentPaymentStatus || "Đang cập nhật" }} · Đơn hàng:
        {{ statuses[currentOrderStatus] || currentOrderStatus || "Đang cập nhật" }}
      </p>
    </section>
  </div>
</template>
