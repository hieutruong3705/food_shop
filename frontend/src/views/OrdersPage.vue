<script setup>
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import { api } from "../services/api";
import { money, date, statuses, useTask } from "../lib/shop";
import TaskState from "../components/TaskState.vue";
const route = useRoute(),
  orders = ref([]),
  order = ref(null),
  { busy, error, message, run } = useTask();
watch(
  () => route.params.id,
  () =>
    run(async () => {
      order.value = null;
      if (route.params.id)
        order.value = await api(`/orders/${route.params.id}`);
      else orders.value = await api("/orders");
    }),
  { immediate: true },
);
function pay() {
  run(async () => {
    const result = await api(`/orders/${order.value.id}/payment`, {
      method: "POST",
    });
    const url = new URL(result.url);
    if (url.origin !== "https://sandbox.vnpayment.vn")
      throw new Error("Địa chỉ thanh toán không hợp lệ");
    window.location.assign(url.href);
  });
}
function cancel() {
  if (!window.confirm("Bạn muốn hủy đơn hàng này?")) return;
  run(async () => {
    order.value = await api(`/orders/${order.value.id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: "CANCELLED" }),
    });
    message.value = "Đã hủy đơn hàng.";
  });
}
</script>
<template>
  <div class="page-heading">
    <span class="eyebrow">TÀI KHOẢN / ĐƠN HÀNG</span>
    <h1>
      {{
        route.params.id ? `Đơn hàng #${route.params.id}` : "Đơn hàng của tôi"
      }}
    </h1>
  </div>
  <TaskState :busy="busy" :error="error" :message="message" /><template
    v-if="order"
    ><div class="two-column">
      <section class="panel">
        <span class="status">{{ statuses[order.status] || order.status }}</span>
        <p>Ngày đặt: {{ date(order.createdAt) }}</p>
        <h2>Thông tin nhận hàng</h2>
        <p>{{ order.receiverName }} · {{ order.receiverPhone }}</p>
        <p>{{ order.receiverAddress }}</p>
        <p v-if="order.note">Ghi chú: {{ order.note }}</p>
        <p>
          {{ order.paymentMethod }} ·
          {{
            order.paymentStatus === "PAID" ? "Đã thanh toán" : "Chưa thanh toán"
          }}
        </p>
        <button
          v-if="
            order.status === 'PENDING' &&
            order.paymentMethod === 'VNPAY' &&
            order.paymentStatus === 'UNPAID'
          "
          :disabled="busy"
          @click="pay"
        >
          Thanh toán VNPay sandbox</button
        ><button
          v-if="order.status === 'PENDING' && order.paymentStatus !== 'PAID'"
          :disabled="busy"
          class="danger"
          @click="cancel"
        >
          Hủy đơn hàng
        </button>
      </section>
      <section class="panel">
        <h2>Chi tiết thanh toán</h2>
        <div v-for="item in order.items" :key="item.id" class="summary-row">
          <RouterLink :to="`/products/${item.productId}`"
            >{{ item.name }}{{ item.variantLabel ? " · " + item.variantLabel : "" }} × {{ item.quantity }}</RouterLink
          ><strong>{{ money(item.price * item.quantity) }}</strong>
        </div>
        <div class="summary-row">
          <span>Phí giao hàng</span><span>{{ money(order.shippingFee) }}</span>
        </div>
        <div class="summary-row">
          <span>Giảm giá {{ order.voucherCode }}</span
          ><span>−{{ money(order.discountAmount) }}</span>
        </div>
        <div class="summary-row total">
          <strong>Tổng thanh toán</strong
          ><strong>{{ money(order.totalPrice) }}</strong>
        </div>
      </section>
    </div>
    <p><RouterLink to="/orders">← Tất cả đơn hàng</RouterLink></p></template
  ><template v-else-if="!route.params.id"
    ><div class="panel table-wrap" v-if="orders.length">
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
    <p class="empty" v-else-if="!busy && !error">
      Bạn chưa có đơn hàng nào.
    </p></template
  >
</template>
