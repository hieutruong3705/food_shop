<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import ProductCard from '../ProductCard.vue';

// Băng chuyền sản phẩm: chia thành trang, tự trượt sau mỗi khoảng thời gian và có nút điều hướng.
// Dừng khi người xem rê chuột, đang focus trong khối, tab bị ẩn hoặc hệ điều hành yêu cầu giảm chuyển động.
const props = defineProps({
  items: { type: Array, default: () => [] },
  perView: { type: Number, default: 5 },
  interval: { type: Number, default: 5000 },
  label: { type: String, default: 'Danh sách sản phẩm' },
});
const page = ref(0), hovered = ref(false), focused = ref(false), reduced = ref(false);
const columns = ref(props.perView);
const pages = computed(() => {
  const size = Math.max(1, columns.value), out = [];
  for (let i = 0; i < props.items.length; i += size) out.push(props.items.slice(i, i + size));
  return out;
});
let timer, motion, media;
function go(next) { if (pages.value.length) page.value = (next + pages.value.length) % pages.value.length; restart(); }
function restart() {
  clearInterval(timer);
  if (pages.value.length > 1 && !hovered.value && !focused.value && !reduced.value && !document.hidden) {
    timer = setInterval(() => { page.value = (page.value + 1) % pages.value.length; }, props.interval);
  }
}
function measure() {
  const width = window.innerWidth;
  columns.value = width <= 920 ? 2 : width <= 1331 ? 3 : props.perView;
}
watch(() => props.items, () => { page.value = 0; restart(); });
watch([hovered, focused, reduced], restart);
watch(pages, () => { if (page.value >= pages.value.length) page.value = 0; });
onMounted(() => {
  motion = window.matchMedia('(prefers-reduced-motion: reduce)');
  reduced.value = motion.matches;
  motion.addEventListener('change', (e) => { reduced.value = e.matches; });
  media = () => measure();
  window.addEventListener('resize', media);
  document.addEventListener('visibilitychange', restart);
  measure(); restart();
});
onUnmounted(() => {
  clearInterval(timer);
  window.removeEventListener('resize', media);
  document.removeEventListener('visibilitychange', restart);
});
</script>

<template>
  <div v-if="items.length" class="product-carousel" :aria-label="label" aria-roledescription="carousel"
    @mouseenter="hovered = true" @mouseleave="hovered = false"
    @focusin="focused = true" @focusout="focused = false">
    <div class="pc-viewport">
      <div class="pc-track" :style="{ transform: `translateX(-${page * 100}%)` }">
        <div v-for="(group, i) in pages" :key="i" class="pc-page" :style="{ '--pc-columns': columns }" :aria-hidden="i !== page">
          <ProductCard v-for="p in group" :key="p.id" :product="p" />
        </div>
      </div>
    </div>
    <template v-if="pages.length > 1">
      <button type="button" class="pc-arrow previous" aria-label="Nhóm sản phẩm trước" @click="go(page - 1)">‹</button>
      <button type="button" class="pc-arrow next" aria-label="Nhóm sản phẩm tiếp theo" @click="go(page + 1)">›</button>
      <div class="pc-dots">
        <button v-for="(group, i) in pages" :key="i" type="button" :class="{ active: i === page }"
          :aria-label="`Xem nhóm ${i + 1} trên ${pages.length}`" :aria-pressed="i === page" @click="go(i)"></button>
      </div>
    </template>
  </div>
</template>
