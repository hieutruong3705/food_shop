<script setup>
import { computed, ref, watch } from 'vue';
import { api } from '../../services/api';
import { loadOffers } from '../../lib/offers';
import ProductCarousel from './ProductCarousel.vue';
import ShopIcon from '../ShopIcon.vue';

// "Deal sốc" và "Hàng mới về" dùng chung một băng chuyền; mỗi lần mở trang thứ tự lại khác nhau
// nên trang chủ không lặp lại y hệt.
const shuffle = (list) => {
  const out = [...list];
  for (let i = out.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [out[i], out[j]] = [out[j], out[i]]; }
  return out;
};
const deals = ref([]), fresh = ref([]), tab = ref('deal'), error = ref('');
const TABS = [
  { key: 'deal', label: 'Deal sốc', icon: 'bolt' },
  { key: 'fresh', label: 'Hàng mới về', icon: 'grid' },
];
const items = computed(() => (tab.value === 'deal' ? deals.value : fresh.value));
const link = computed(() => (tab.value === 'deal' ? '/products?sort=popular' : '/products?sort=newest'));

async function load() {
  try {
    const [offers, newest] = await Promise.all([loadOffers().catch(() => null), api('/products?size=24&sort=newest')]);
    fresh.value = shuffle(newest.content || []).slice(0, 20);
    const running = (offers?.promotions || []).filter((x) => x.hasAvailableProducts);
    const pages = await Promise.all(running.slice(0, 4).map((x) => api(`/products?size=12&promotionId=${x.id}`).catch(() => null)));
    const seen = new Set(), pool = [];
    pages.filter(Boolean).flatMap((p) => p.content || []).forEach((p) => {
      if (!seen.has(p.id) && p.discountedPrice < p.price) { seen.add(p.id); pool.push(p); }
    });
    deals.value = shuffle(pool).slice(0, 20);
    if (!deals.value.length) { tab.value = 'fresh'; }
  } catch (e) { error.value = e.message; }
}
load();
watch(tab, () => { /* đổi tab thì trộn lại để không nhàm */
  if (tab.value === 'deal') deals.value = shuffle(deals.value);
  else fresh.value = shuffle(fresh.value);
});
</script>

<template>
  <section v-if="deals.length || fresh.length" class="deal-board home-block" aria-labelledby="deal-title">
    <div class="deal-head">
      <div class="deal-tabs" role="tablist" aria-label="Nhóm ưu đãi">
        <button v-for="t in TABS" :key="t.key" role="tab" type="button" :aria-selected="tab === t.key"
          :class="{ active: tab === t.key }" :disabled="t.key === 'deal' ? !deals.length : !fresh.length"
          @click="tab = t.key"><ShopIcon :name="t.icon" />{{ t.label }}</button>
      </div>
      <h2 id="deal-title" class="visually-hidden">Ưu đãi và hàng mới</h2>
      <RouterLink :to="link" class="deal-all">Xem tất cả <ShopIcon name="arrow" /></RouterLink>
    </div>
    <ProductCarousel :items="items" :per-view="5" :interval="5000" :label="tab === 'deal' ? 'Deal sốc' : 'Hàng mới về'" />
  </section>
</template>
