<script setup>
import { onMounted, ref } from 'vue';
import ProductCard from '../ProductCard.vue';
import GroupBanner from './GroupBanner.vue';
import { api } from '../../services/api';
import { recentProducts } from '../../lib/recent';

// Products this shopper opened lately, read from this browser. The row ends with a one-cell
// advertising banner (placement HOME_RECENT) when the shop has one.
const items = ref([]), banner = ref(null);
onMounted(async () => {
  items.value = recentProducts().slice(0, 4);
  if (!items.value.length) return;
  try { const list = await api('/banners?placement=HOME_RECENT'); banner.value = Array.isArray(list) && list[0]?.imageUrl ? list[0] : null; } catch { /* The row simply shows products only. */ }
});
</script>

<template>
  <section v-if="items.length" class="home-block" aria-labelledby="recent-title">
    <div class="retail-section-title"><h2 id="recent-title">Bạn vừa xem</h2></div>
    <div class="product-grid recent-grid" :class="{ 'has-banner': banner }"><ProductCard v-for="p in items" :key="p.id" :product="p" /><GroupBanner v-if="banner" :banner="banner" :cells="1" /></div>
  </section>
</template>
