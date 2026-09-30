<script setup>
import { ref, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "../services/api";
import { useAuth } from "../stores/auth";
import { money, imageUrl, imageError, useTask, date, discountPercent } from "../lib/shop";
import TaskState from "../components/TaskState.vue";
import ProductCard from "../components/ProductCard.vue";
const route = useRoute(),
  router = useRouter(),
  auth = useAuth(),
  detail = ref(null),
  reviews = ref([]),
  reviewSummary = ref({average:0,count:0,canReview:false,purchased:false}),
  related = ref([]),
  quantity = ref(1),
  selectedOptions = ref({}),
  selected = ref(""),
  editingReview = ref(null),
  review = ref({ rating: 5, content: "" });
const { busy, error, message, run } = useTask();
const variant = computed(() => detail.value?.variants?.find(v => Object.entries(v.options).every(([key, value]) => selectedOptions.value[key] === value) && Object.entries(selectedOptions.value).every(([key, value]) => v.options[key] === value)));
const variantId = computed(() => variant.value?.id ?? null);
const isColor = name => /màu|color|colour/i.test(name);
const optionGroups = computed(() => {
  const variants = detail.value?.variants || [];
  const names = [...new Set(variants.flatMap(v => Object.keys(v.options)))].sort((a, b) => Number(isColor(b)) - Number(isColor(a)) || a.localeCompare(b, 'vi'));
  return names.map((name, index) => ({
    name,
    values: [...new Set(variants.map(v => v.options[name]).filter(Boolean))].map(value => {
      const candidates = variants.filter(v => v.options[name] === value && names.slice(0, index).every(key => !selectedOptions.value[key] || v.options[key] === selectedOptions.value[key]));
      return { value, available: candidates.some(v => v.quantity > 0), image: candidates.find(v => v.quantity > 0)?.imageUrl || candidates[0]?.imageUrl || detail.value.images[0] };
    }),
  }));
});
function chooseOption(name, value) {
  const next = { ...selectedOptions.value, [name]: value };
  const index = optionGroups.value.findIndex(g => g.name === name);
  for (const group of optionGroups.value.slice(index + 1)) {
    const compatible = detail.value.variants.some(v => v.quantity > 0 && Object.entries(next).every(([key, chosen]) => v.options[key] === chosen));
    if (!compatible) delete next[group.name];
  }
  selectedOptions.value = next;
  quantity.value = 1;
  const candidate = detail.value.variants.find(v => Object.entries(next).every(([key, chosen]) => v.options[key] === chosen));
  selected.value = candidate?.imageUrl || detail.value.images[0] || '';
}
const availableStock = computed(() => detail.value?.hasVariants ? (variant.value?.quantity || 0) : (detail.value?.product.quantity || 0));
const displayedPrice = computed(() => variant.value?.discountedPrice ?? detail.value?.product.discountedPrice);
const listedPrice = computed(() => variant.value?.price ?? detail.value?.product.price);
watch(
  () => route.params.id,
  () =>
    run(async () => {
      detail.value = null;
      selectedOptions.value = {};
      reviews.value = [];
      related.value = [];
      editingReview.value = null;
      review.value = { rating: 5, content: "" };
      detail.value = await api(`/products/${route.params.id}`);
      selected.value = detail.value.images[0];
      await loadReviews();
      const p = await api(
        `/products?size=5&categoryId=${detail.value.categoryId || ""}`,
      );
      related.value = p.content.filter((p) => p.id !== detail.value.product.id);
      quantity.value = 1;
    }),
  { immediate: true },
);
function add() {
  if (!auth.user)
    return router.push({ path: "/login", query: { redirect: route.fullPath } });
  run(async () => {
    await api("/cart/items", {
      method: "POST",
      body: JSON.stringify({
        productId: detail.value.product.id,
        quantity: quantity.value,
        variantId: variantId.value,
      }),
    });
    message.value = "Đã thêm sản phẩm vào giỏ hàng.";
  });
}
function submitReview() {
  run(async () => {
    await api(`/products/${route.params.id}/reviews${editingReview.value ? '/' + editingReview.value : ''}`, {
      method: editingReview.value ? "PUT" : "POST",
      body: JSON.stringify(review.value),
    });
    await loadReviews();
    review.value.content = "";
    editingReview.value = null;
    message.value = "Đã lưu đánh giá.";
  });
}
function editReview(r) {
  editingReview.value = r.id;
  review.value = { rating: r.rating, content: r.content };
}
function removeReview(r) {
  if (!window.confirm("Xóa đánh giá của bạn?")) return;
  run(async () => {
    await api(`/products/${route.params.id}/reviews/${r.id}`, { method: "DELETE" });
    await loadReviews();
    editingReview.value = null;
    review.value = { rating: 5, content: "" };
    message.value = "Đã xóa đánh giá.";
  });
}
async function loadReviews(){
  [reviews.value,reviewSummary.value]=await Promise.all([api(`/products/${route.params.id}/reviews`),api(`/products/${route.params.id}/reviews/summary`)]);
}
</script>
<template>
  <TaskState :busy="busy" :error="error" :message="message" /><template
    v-if="detail"
    ><div class="breadcrumb">
      <RouterLink to="/products">Sản phẩm</RouterLink> /
      {{ detail.product.name }}
    </div>
    <section class="detail-grid">
      <div>
        <div class="detail-image">
          <img
            :src="imageUrl(selected)"
            :alt="detail.product.name"
            @error="imageError"
          />
        </div>
        <div class="thumbnails">
          <button
            v-for="image in detail.images"
            :key="image"
            @click="selected = image"
          >
            <img
              :src="imageUrl(image)"
              :alt="detail.product.name"
              @error="imageError"
            />
          </button>
        </div>
      </div>
      <div class="detail-copy">
        <span class="eyebrow"
          >{{ detail.product.brand }} / {{ detail.product.category }}</span
        >
        <h1>{{ detail.product.name }}</h1>
        <p>{{ detail.product.shortDescription }}</p>
        <div class="price">
          <small v-if="detail.hasVariants && !variant">Từ </small>{{ money(displayedPrice) }}
          <del v-if="listedPrice > displayedPrice">{{
            money(listedPrice)
          }}</del>
          <span v-if="listedPrice > displayedPrice" class="discount-label">Giảm {{ discountPercent(listedPrice, displayedPrice) }}%</span>
        </div>
        <p>
          {{
            detail.hasVariants && !variant ? "Chọn phiên bản để xem tồn kho" : availableStock
              ? `Còn ${availableStock} sản phẩm`
              : "Tạm hết hàng"
          }}
        </p>
        <div v-if="detail.hasVariants" class="variant-picker" aria-label="Chọn phiên bản">
          <fieldset v-for="group in optionGroups" :key="group.name" class="option-group">
            <legend>{{ group.name }}</legend>
            <div class="option-values">
              <label v-for="option in group.values" :key="option.value" class="option-chip" :class="{ chosen: selectedOptions[group.name] === option.value, unavailable: !option.available }">
                <input type="radio" :name="'option-' + group.name" :value="option.value" :checked="selectedOptions[group.name] === option.value" :disabled="!option.available || busy" :aria-label="group.name + ': ' + option.value" @change="chooseOption(group.name, option.value)" />
                <img v-if="isColor(group.name)" :src="imageUrl(option.image)" alt="" @error="imageError" />
                <span>{{ option.value }}<small v-if="!option.available">Không có sẵn</small></span>
                <span v-if="selectedOptions[group.name] === option.value" class="option-check" aria-hidden="true">✓</span>
              </label>
            </div>
          </fieldset>
          <p v-if="variant" class="chosen-variant">Đã chọn: {{ variant.label }}</p>
        </div>
        <div class="buy-row">
          <input
            v-model.number="quantity"
            type="number"
            min="1"
            :max="Math.min(availableStock, 100)"
            aria-label="Số lượng"
          /><button :disabled="busy || !availableStock" @click="add">
            Thêm vào giỏ hàng ↗
          </button>
        </div>
        <RouterLink to="/cart">Xem giỏ hàng →</RouterLink>
        <div class="detail-meta">
          <p>SKU: {{ variant?.sku || detail.sku || "Đang cập nhật" }}</p>
          <p>Bảo hành: {{ detail.warrantyMonths ?? "—" }} tháng</p>
          <p>Miễn phí vận chuyển đơn từ 1.000.000₫</p>
        </div>
      </div>
    </section>
    <div class="two-column section">
      <article class="panel">
        <h2>Thông tin sản phẩm</h2>
        <p class="prewrap">{{ detail.detailDescription }}</p>
      </article>
      <article class="panel">
        <h2>Thông số kỹ thuật</h2>
        <dl class="specs">
          <template v-for="s in detail.specifications" :key="s.name"
            ><dt>{{ s.name }}</dt>
            <dd>{{ s.value }}</dd></template
          >
        </dl>
      </article>
    </div>
    <section class="panel section">
      <h2>Đánh giá khách hàng</h2>
      <p class="review-summary" aria-label="Tổng hợp đánh giá"><strong>{{ Number(reviewSummary.average).toFixed(1) }}/5 ★</strong> · {{ reviewSummary.count }} đánh giá</p>
      <p v-if="!reviews.length" class="muted">
        Chưa có đánh giá cho sản phẩm này.
      </p>
      <article v-for="r in reviews" :key="r.id" class="review">
        <strong>{{ r.author }} · {{ "★".repeat(r.rating) }}</strong
        ><small>{{ date(r.createdAt) }}</small>
        <p>{{ r.content }}</p>
        <div v-if="r.editable" class="actions"><button :disabled="busy" @click="editReview(r)">Sửa đánh giá</button><button class="secondary" :disabled="busy" @click="removeReview(r)">Xóa đánh giá</button></div>
      </article>
      <p v-if="auth.user && !reviewSummary.purchased" class="muted">Bạn có thể đánh giá sau khi đơn mua sản phẩm này đã hoàn thành.</p>
      <form v-if="auth.user && (editingReview || reviewSummary.canReview)" @submit.prevent="submitReview" class="form">
        <label
          >Đánh giá sau khi nhận hàng<select v-model.number="review.rating">
            <option v-for="n in 5" :key="n" :value="n">{{ n }} sao</option>
          </select></label
        ><label
          >Nội dung<textarea
            v-model="review.content"
            required
            maxlength="2000"
          /></label
        ><button :disabled="busy">{{ editingReview ? 'Lưu đánh giá' : 'Gửi đánh giá' }}</button>
      </form>
      <RouterLink v-if="!auth.user" to="/login">Đăng nhập để đánh giá</RouterLink>
    </section>
    <section class="section">
      <h2>Có thể bạn quan tâm</h2>
      <div class="product-grid">
        <ProductCard v-for="p in related" :key="p.id" :product="p" />
      </div></section
  ></template>
</template>
