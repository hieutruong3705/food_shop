<script setup>
import { useRoute } from "vue-router";
const route = useRoute();
</script>
<template>
  <section class="empty">
    <h1>Trở lại từ VNPay sandbox</h1>
    <p>
      Trạng thái đơn hàng được cập nhật sau khi máy chủ nhận xác nhận thanh
      toán.
    </p>
    <RouterLink
      v-if="/^\d+$/.test(String(route.query.vnp_TxnRef || ''))"
      class="button"
      :to="`/orders/${route.query.vnp_TxnRef}`"
      >Kiểm tra đơn hàng</RouterLink
    ><RouterLink v-else class="button" to="/orders"
      >Xem đơn hàng của tôi</RouterLink
    >
  </section>
</template>
