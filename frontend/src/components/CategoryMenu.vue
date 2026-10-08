<script>
// The menu is mounted with v-if, so the cache lives outside the component: reopening it
// must not refetch panels the visitor already looked at.
const facetCache = new Map();
</script>
<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { api } from '../services/api';
import { categoryImage } from '../lib/presentation';
import { imageUrl, imageError, money, rateText } from '../lib/shop';
import ShopIcon from './ShopIcon.vue';
import BrandMark from './BrandMark.vue';
import { useRoute } from 'vue-router';
const route = useRoute();
const props = defineProps({ categories: Array });
const emit = defineEmits(['close']);
const selected = ref(null), facets = ref(null), error = ref(''), busy = ref(false);
const minPrice = ref(0), maxPrice = ref(0);
let requestId = 0, hoverTimer, alive = true;
function applyFacets(category, data) {
  selected.value = category;
  facets.value = data;
  minPrice.value = data.minPrice;
  maxPrice.value = data.maxPrice;
}
async function select(category) {
  if (selected.value?.id === category.id && facets.value) return;
  const cached = facetCache.get(category.id);
  if (cached) {
    requestId++;
    applyFacets(category, cached);
    error.value = '';
    busy.value = false;
    return;
  }
  // The old panel stays visible, dimmed, so the menu does not collapse and reflow on every hover.
  selected.value = category; error.value = ''; busy.value = true;
  const id = ++requestId;
  try { const data = await api('/catalog/menu?categoryId=' + category.id); if (id === requestId) { facetCache.set(category.id, data); applyFacets(category, data); } }
  catch(e) { if (id === requestId) error.value = e.message; }
  finally { if (id === requestId) busy.value = false; }
}
function scheduleSelect(category) {
  clearTimeout(hoverTimer);
  hoverTimer = setTimeout(() => select(category), 120);
}
function selectNow(category) {
  clearTimeout(hoverTimer);
  select(category);
}
async function prefetch(list) {
  for (const c of list) {
    if (!alive) return;
    if (facetCache.has(c.id)) continue;
    try { facetCache.set(c.id, await api('/catalog/menu?categoryId=' + c.id)); }
    catch { /* A hover on this category retries and reports the error there. */ }
    await new Promise(resolve => setTimeout(resolve, 60));
  }
}
watch(() => props.categories, async list => {
  if (!list?.length) return;
  if (!selected.value) await select(list.find(c=>String(c.id)===String(route.query.categoryId))||list[0]);
  prefetch(list);
}, { immediate: true });
function link(query = {}) {
  const inherited = route.path === '/products' && String(route.query.categoryId) === String(selected.value?.id) ? {...route.query} : {};
  delete inherited.page;
  if(query.brandId)delete inherited.brandIds;
  return {path:'/products',query:{...inherited,...query,categoryId:selected.value?.id}};
}
function escape(event) { if(event.key === 'Escape') emit('close'); }
onMounted(() => document.addEventListener('keydown', escape));
onUnmounted(() => { alive = false; requestId++; clearTimeout(hoverTimer); document.removeEventListener('keydown', escape); });
</script>
<template>
  <div class="mega-backdrop" @click="emit('close')" aria-hidden="true"></div>
  <div id="header-categories" class="mega-menu" aria-label="Danh mục và bộ lọc">
    <div class="mega-category-list">
      <div v-for="c in categories" :key="c.id" :class="{ active: selected?.id === c.id }" @mouseenter="scheduleSelect(c)" @focusin="selectNow(c)">
        <RouterLink :to="{ path: '/products', query: { categoryId: c.id } }" @click="emit('close')"><img :src="c.image ? imageUrl(c.image) : categoryImage(c.name)" alt="" @error="event => event.target.src = categoryImage(c.name)" />{{ c.name }}</RouterLink>
        <button :aria-label="`Xem nhóm ${c.name}`" :aria-expanded="selected?.id === c.id" @click="selectNow(c)"><ShopIcon name="chevron" /></button>
      </div>
      <RouterLink to="/products" class="menu-shortcut" @click="emit('close')">Tất cả sản phẩm →</RouterLink>
      <RouterLink to="/products?sort=newest" class="menu-shortcut" @click="emit('close')">Hàng mới về →</RouterLink>
    </div>
    <div class="mega-detail">
      <div class="mega-title"><RouterLink :to="link()" @click="emit('close')">{{ selected?.name || 'Danh mục sản phẩm' }} →</RouterLink><button aria-label="Đóng danh mục" class="text-button" @click="emit('close')">✕</button></div>
      <p v-if="busy" role="status" class="mega-loading">Đang tải danh mục…</p><p v-if="error" role="alert">{{ error }} <button class="text-button" @click="select(selected)">Thử lại</button></p>
      <div v-if="facets" class="mega-columns" :class="{ loading: busy }">
        <section class="mega-list" aria-label="Top bán chạy"><h3>Top bán chạy</h3><RouterLink v-for="entry in facets.bestSellers" :key="entry.product.id" :to="`/products/${entry.product.id}`" @click="emit('close')"><img :src="imageUrl(entry.product.image)" alt="" loading="lazy" @error="imageError" /><span class="mega-name">{{ entry.product.name }}</span><small>{{ money(entry.product.discountedPrice) }}<template v-if="entry.product.discountRate > 0"> · <b class="mega-tag">Giảm {{ rateText(entry.product.discountRate) }}%</b></template> · Đã bán {{ entry.units }}</small></RouterLink><p v-if="!facets.bestSellers.length" class="muted">Chưa có dữ liệu bán hàng.</p></section>
        <section class="mega-list" aria-label="Sản phẩm mới"><h3>Sản phẩm mới</h3><RouterLink v-for="entry in facets.newest" :key="entry.product.id" :to="`/products/${entry.product.id}`" @click="emit('close')"><img :src="imageUrl(entry.product.image)" alt="" loading="lazy" @error="imageError" /><span class="mega-name">{{ entry.product.name }}</span><small>{{ money(entry.product.discountedPrice) }}<template v-if="entry.product.discountRate > 0"> · <b class="mega-tag">Giảm {{ rateText(entry.product.discountRate) }}%</b></template></small></RouterLink><p v-if="!(facets.newest || []).length" class="muted">Chưa có sản phẩm.</p></section>
        <section><h3>Chọn thương hiệu</h3><div class="mega-brands"><RouterLink v-for="b in facets.brands" :key="b.id" :to="link({brandId:b.id})" @click="emit('close')"><BrandMark :brand="b" /></RouterLink></div><p v-if="!facets.brands.length" class="muted">Danh mục chưa gán thương hiệu.</p></section>
        <section><h3>Mức giá</h3><form class="mega-price" @submit.prevent="$router.push(link({minPrice,maxPrice})); emit('close')"><label>Từ {{ money(minPrice) }}<input aria-label="Giá thấp nhất trong menu" type="range" :min="facets.minPrice" :max="facets.maxPrice" v-model.number="minPrice" @input="maxPrice = Math.max(minPrice,maxPrice)" /></label><label>Đến {{ money(maxPrice) }}<input aria-label="Giá cao nhất trong menu" type="range" :min="facets.minPrice" :max="facets.maxPrice" v-model.number="maxPrice" @input="minPrice = Math.min(minPrice,maxPrice)" /></label><button>Áp dụng giá</button></form></section>
      </div>
    </div>
  </div>
</template>
