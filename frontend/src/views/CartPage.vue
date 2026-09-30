<script setup>
import { ref, onMounted, computed, watch } from "vue";
import { api } from "../services/api";
import { money, imageUrl, imageError, useTask } from "../lib/shop";
import TaskState from "../components/TaskState.vue";
const cart = ref(null),
  selected = ref([]),
  { busy, error, run } = useTask();
onMounted(() =>
  run(async () => {
    cart.value = await api("/cart");
    selected.value = cart.value.items.filter(canBuy).map((i) => i.id);
  }),
);
const selectedItems = computed(
  () => cart.value?.items.filter((i) => selected.value.includes(i.id) && canBuy(i)) || [],
);
function canBuy(item) { return item.active && item.quantity > 0 && item.quantity <= Math.min(item.stock,100); }
watch(cart, () => { selected.value = selected.value.filter(id => cart.value?.items.some(i => i.id === id && canBuy(i))); });
const total = computed(() =>
  selectedItems.value.reduce((sum, i) => sum + i.price * i.quantity, 0),
);
function update(item, event) {
  const q = Number(event.target.value);
  run(async () => {
    try { cart.value = await api(`/cart/items/${item.id}`, {
        method: "PATCH",
        body: JSON.stringify({ quantity: q }),
      }); }
    finally { event.target.value = cart.value?.items.find(i => i.id === item.id)?.quantity ?? item.quantity; }
  });
}
function remove(item) {
  run(
    async () =>
      (cart.value = await api(`/cart/items/${item.id}`, { method: "DELETE" })),
  );
}
</script>
<template>
  <div class="page-heading">
    <span class="eyebrow">THIẾT BỊ BẠN ĐÃ CHỌN</span>
    <h1>Giỏ hàng của bạn</h1>
  </div>
  <TaskState :busy="busy" :error="error" />
  <div v-if="cart?.items.length" class="checkout-grid">
    <div class="panel">
      <article v-for="item in cart.items" :key="item.id" class="cart-line">
        <input
          type="checkbox"
          v-model="selected"
          :value="item.id"
          :disabled="busy || !canBuy(item)"
          :aria-label="`Chọn ${item.name}`"
        /><img
          :src="imageUrl(item.image)"
          :alt="item.name"
          @error="imageError"
        />
        <div>
          <RouterLink v-if="item.active" :to="`/products/${item.productId}`"
            ><strong>{{ item.name }}{{ item.variantLabel ? " · " + item.variantLabel : "" }}</strong></RouterLink
          >
          <strong v-else>{{ item.name }}{{ item.variantLabel ? " · " + item.variantLabel : "" }}</strong>
          <p>{{ money(item.price) }}</p>
          <small
            v-if="!item.active || item.stock < item.quantity"
            class="error-text"
            >{{ !item.active ? 'Sản phẩm hoặc phiên bản đã ngừng bán.' : item.stock === 0 ? 'Sản phẩm đã hết hàng.' : 'Số lượng vượt tồn kho; hãy giảm số lượng để tiếp tục.' }}</small
          >
        </div>
        <input
          type="number"
          :value="item.quantity"
          min="1"
          :max="Math.min(item.stock, 100)"
          :disabled="busy || !item.active || item.stock < 1"
          :aria-label="`Số lượng ${item.name}`"
          @change="update(item, $event)"
        /><button class="text-button" :disabled="busy" @click="remove(item)">
          Xóa
        </button>
      </article>
    </div>
    <aside class="panel summary">
      <h2>Tóm tắt giỏ hàng</h2>
      <p>{{ selectedItems.length }} sản phẩm được chọn</p>
      <div class="summary-row">
        <span>Tạm tính</span><strong>{{ money(total) }}</strong>
      </div>
      <p class="muted">Phí giao hàng và giảm giá được xác định khi đặt hàng.</p>
      <p v-if="!selectedItems.length" class="error-text">Chọn ít nhất một sản phẩm còn bán và đủ tồn kho để đặt hàng.</p>
      <RouterLink
        v-if="selectedItems.length"
        class="button"
        :to="{
          path: '/checkout',
          query: { items: selectedItems.map((i) => i.id).join(',') },
        }"
        >Tiến hành đặt hàng →</RouterLink
      >
    </aside>
  </div>
  <div v-else-if="cart" class="empty">
    <h2>Giỏ hàng đang trống</h2>
    <RouterLink class="button" to="/products">Khám phá sản phẩm</RouterLink>
  </div>
</template>
