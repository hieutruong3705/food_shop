<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { api } from "../services/api";
import { money, useTask } from "../lib/shop";
import TaskState from "../components/TaskState.vue";
const router = useRouter(),
  route = useRoute(),
  cart = ref(null),
  quote = ref(null),
  paymentOptions = ref({}),
  form = reactive({
    receiverName: "",
    receiverPhone: "",
    receiverAddress: "",
    note: "",
    voucherCode: "",
    paymentMethod: "COD",
    cartItemIds: route.query.items
      ? String(route.query.items)
          .split(",")
          .map(Number)
          .sort((a, b) => a - b)
      : null,
  }),
  { busy, error, message, run } = useTask();
const shipping = computed(() => quote.value?.shippingFee || 0);
async function refreshQuote() {
  quote.value = await api(
    "/cart/quote?voucherCode=" +
      encodeURIComponent(form.voucherCode.trim()) +
      (form.cartItemIds ? "&cartItemIds=" + form.cartItemIds.join(",") : ""),
  );
  message.value = "Đã cập nhật tổng thanh toán.";
}
onMounted(() =>
  run(async () => {
    const [c, p, options] = await Promise.all([
      api("/cart"),
      api("/account"),
      api("/payments/options"),
    ]);
    cart.value = {
      ...c,
      items: form.cartItemIds
        ? c.items.filter((i) => form.cartItemIds.includes(i.id))
        : c.items,
    };
    paymentOptions.value = options;
    form.receiverName = p.fullName;
    form.receiverPhone = p.phone || "";
    form.receiverAddress = p.address || "";
    if (cart.value.items.length) await refreshQuote();
  }),
);
function submit() {
  run(async () => {
    if (
      !quote.value ||
      quote.value.voucherCode !== form.voucherCode.trim().toUpperCase()
    ) {
      await refreshQuote();
      message.value =
        "Đã áp dụng mã. Kiểm tra tổng tiền rồi xác nhận đặt hàng.";
      return;
    }
    let attempt;
    try {
      attempt = JSON.parse(sessionStorage.getItem("checkout-attempt"));
    } catch {}
    const body = JSON.stringify(form);
    if (!attempt || attempt.body !== body)
      attempt = { key: crypto.randomUUID(), body };
    sessionStorage.setItem("checkout-attempt", JSON.stringify(attempt));
    const order = await api("/orders", {
      method: "POST",
      headers: { "Idempotency-Key": attempt.key },
      body,
    });
    sessionStorage.removeItem("checkout-attempt");
    router.push(`/orders/${order.id}`);
  });
}
</script>
<template>
  <div class="page-heading">
    <h1>Thông tin đặt hàng</h1>
    <p>Kiểm tra thông tin trước khi xác nhận.</p>
  </div>
  <TaskState :busy="busy" :error="error" :message="message" />
  <form
    v-if="cart?.items.length"
    class="checkout-grid"
    @submit.prevent="submit"
  >
    <section class="panel form">
      <h2>Địa chỉ nhận hàng</h2>
      <label
        >Họ tên<input
          v-model="form.receiverName"
          required
          maxlength="100"
          autocomplete="name" /></label
      ><label
        >Số điện thoại<input
          v-model="form.receiverPhone"
          required
          pattern="[+0-9 ()\-]{9,20}"
          autocomplete="tel" /></label
      ><label
        >Địa chỉ đầy đủ<textarea
          v-model="form.receiverAddress"
          required
          maxlength="500"
          autocomplete="street-address"
        />
      </label>
      <h2>Thanh toán</h2>
      <label>Ghi chú giao hàng<textarea v-model="form.note" maxlength="1000" placeholder="Yêu cầu giao hàng (nếu có)" /></label>
      <label class="payment-choice"
        ><input type="radio" v-model="form.paymentMethod" value="COD" /> Thanh
        toán khi nhận hàng (COD)</label
      ><label v-if="paymentOptions.vnpay" class="payment-choice"
        ><input type="radio" v-model="form.paymentMethod" value="VNPAY" /> VNPay
        sandbox (thử nghiệm)</label
      ><label
        >Mã giảm giá<input
          v-model="form.voucherCode"
          maxlength="50"
          placeholder="Nhập mã nếu có" /></label
      ><button
        type="button"
        class="secondary"
        :disabled="busy"
        @click="run(refreshQuote)"
      >
        Áp dụng mã giảm giá
      </button>
    </section>
    <aside class="panel summary">
      <h2>Đơn hàng của bạn</h2>
      <div v-for="item in cart.items" :key="item.id" class="summary-row">
        <span>{{ item.name }}{{ item.variantLabel ? " · " + item.variantLabel : "" }} × {{ item.quantity }}</span
        ><strong>{{ money(item.price * item.quantity) }}</strong>
      </div>
      <div class="summary-row">
        <span>Vận chuyển</span><strong>{{ money(shipping) }}</strong>
      </div>
      <div class="summary-row">
        <span>Giảm giá</span
        ><strong>−{{ money(quote?.discountAmount) }}</strong>
      </div>
      <div class="summary-row total">
        <span>Tổng thanh toán</span
        ><strong>{{ money(quote?.totalPrice) }}</strong>
      </div>
      <p class="muted">
        Giá và tồn kho được kiểm tra lại khi xác nhận đơn hàng.
      </p>
      <button :disabled="busy">Xác nhận đặt hàng →</button>
    </aside>
  </form>
  <div v-else-if="cart" class="empty">
    Giỏ hàng trống. <RouterLink to="/products">Chọn sản phẩm</RouterLink>
  </div>
</template>
