<script setup>
import { ref, watch, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../services/api';
import { money } from '../lib/shop';
import ProductCard from '../components/ProductCard.vue';
import TaskState from '../components/TaskState.vue';
import BrandMark from '../components/BrandMark.vue';
import FilterRange from '../components/FilterRange.vue';
const route = useRoute(), router = useRouter();
const result = ref(null), categories = ref([]), facets = ref(null), filters = ref({}), busy = ref(false), error = ref('');
// Danh mục nhiều thông số sinh ra rất nhiều nhóm lọc; chỉ mở sẵn vài nhóm đầu để cột lọc không dài vô tận.
const SPEC_LIMIT = 6, allSpecs = ref(false), PAGE_SIZE = 20;
const shownSpecs = computed(() => allSpecs.value ? (facets.value?.specs || []) : (facets.value?.specs || []).slice(0, SPEC_LIMIT));
const filtersOpen = ref(false);
const priceLow=ref(0),priceHigh=ref(0);
const hiddenSpecs=['phiên bản','kết nối','bảo hành','version','connection','warranty'];
let requestId = 0;
const array = value => value == null ? [] : Array.isArray(value) ? value : [value];
const categoryName = computed(() => categories.value.find(c => String(c.id) === String(route.query.categoryId))?.name);
function queryString(query) { const params = new URLSearchParams(); for (const [k,v] of Object.entries(query)) for(const item of array(v)) if(item !== '') params.append(k,item); return params.toString(); }
async function load() {
  const obsolete=Object.keys(route.query).filter(key=>key.startsWith('spec.')&&hiddenSpecs.includes(key.slice(5).trim().toLowerCase()));
  if(obsolete.length){const query={...route.query};for(const key of obsolete)delete query[key];await router.replace({query});return;}
  const id = ++requestId;
  filters.value = {...route.query, categoryId:route.query.categoryId || '', sort:route.query.sort || (route.query.categoryId ? 'newest' : 'featured'), brandIds: array(route.query.brandIds || route.query.brandId).map(String)};
  delete filters.value.brandId;
  busy.value = true; error.value = ''; facets.value = null;
  try {
    // Lưới danh mục rộng 4 cột; 20 sản phẩm lấp đúng 5 hàng nên không có hàng cụt.
    // Không chọn danh mục thì xếp "Nổi bật" để các loại hàng xen kẽ, tránh dồn một lô nhập.
    const query = { size: PAGE_SIZE, sort: route.query.categoryId ? 'newest' : 'featured', ...route.query };
    const [p,f] = await Promise.all([api('/products?' + queryString(query)), api('/catalog/facets?' + queryString({categoryId:route.query.categoryId}))]);
    if(id === requestId) { result.value = p; facets.value = f;priceLow.value=Number(route.query.minPrice??f.minPrice);priceHigh.value=Number(route.query.maxPrice??f.maxPrice); }
  } catch(e) { if(id === requestId) { error.value = e.message; result.value = null; } }
  finally { if(id === requestId) busy.value = false; }
}
// Brand, price, stock and specification choices are a draft until "Áp dụng bộ lọc", so a shopper can
// pick several at once; category, sort, paging, chips and "Xóa bộ lọc" still act straight away.
function cleaned(source) {
  const q = {...source}; delete q.brandId; delete q.page;
  for(const k of Object.keys(q)) if(q[k] == null || q[k] === '' || (Array.isArray(q[k]) && !q[k].length) || q[k] === false) delete q[k];
  return q;
}
function apply(page = 0) {
  const q = cleaned(filters.value); if (page) q.page = page;
  filtersOpen.value = false;
  router.push({query:q});
}
const signature = q => JSON.stringify(Object.keys(q).sort().map(k => [k, array(q[k]).map(String).sort()]));
const pending = computed(() => signature(cleaned(filters.value)) !== signature(cleaned({...route.query, sort: route.query.sort || (route.query.categoryId ? 'newest' : 'featured'), brandIds: array(route.query.brandIds || route.query.brandId)})));
function changeCategory() { const categoryId = filters.value.categoryId; filters.value = {categoryId, sort:filters.value.sort || 'newest'}; apply(); }
function toggle(key,value) { const list = array(filters.value[key]).map(String); filters.value[key] = list.includes(String(value)) ? list.filter(x => x !== String(value)) : [...list,String(value)]; }
// A bound left at the catalog's own minimum or maximum is no filter, so it stays out of the URL.
function setPrice({low,high}) {
  const f=facets.value;filters.value.minPrice=low>f.minPrice?low:'';filters.value.maxPrice=high<f.maxPrice?high:'';
}
function clear() { filters.value = {categoryId:route.query.categoryId}; apply(); }
const chips = computed(() => {
  const list = Object.entries(route.query).filter(([key]) => key.startsWith('spec.') || ['inStock','brandIds','brandId','promotionId'].includes(key)).flatMap(([key,v]) => array(v).map(value => ({key,value,label:key.startsWith('spec.') ? `${key.slice(5)}: ${value}` : key === 'promotionId' ? 'Khuyến mãi đang chọn' : key === 'inStock' ? 'Sẵn hàng' : facets.value?.brands.find(b => String(b.id) === value)?.name || 'Thương hiệu'})));
  const f = facets.value, q = route.query;
  if (f && (q.minPrice != null || q.maxPrice != null)) list.push({key:'price',value:'',label:`Giá ${money(q.minPrice ?? f.minPrice)} – ${money(q.maxPrice ?? f.maxPrice)}`});
  if (f && (q.minWarranty != null || q.maxWarranty != null)) list.push({key:'warranty',value:'',label:`Bảo hành ${q.minWarranty ?? f.minWarranty}–${q.maxWarranty ?? f.maxWarranty} tháng`});
  return list;
});
function remove(chip) {
  if (chip.key === 'price') { filters.value.minPrice = ''; filters.value.maxPrice = ''; return apply(); }
  if (chip.key === 'warranty') { filters.value.minWarranty = ''; filters.value.maxWarranty = ''; return apply(); }
  const key = chip.key === 'brandId' ? 'brandIds' : chip.key; filters.value[key] = array(filters.value[key]).filter(v => String(v) !== String(chip.value)); apply(); }
watch(() => route.query, load);
onMounted(async () => { load(); try { categories.value = await api('/categories'); } catch(e) { error.value = e.message; } });
</script>
<template>
  <div class="page-heading"><span class="eyebrow">BỘ SƯU TẬP TECHSHOP</span><h1>{{ categoryName || 'Khám phá công nghệ' }}</h1><p>Chọn hãng, mức giá và thông số phù hợp với bạn.</p></div>
  <div class="faceted-catalog">
    <aside class="catalog-filters" :class="{ expanded: filtersOpen }" aria-label="Bộ lọc sản phẩm">
      <div class="filter-title"><h2>Bộ lọc</h2><button class="mobile-filter-toggle text-button" :aria-expanded="filtersOpen" @click="filtersOpen = !filtersOpen">{{ filtersOpen ? 'Thu gọn' : 'Mở bộ lọc' }}</button><button class="text-button" @click="clear">Xóa bộ lọc</button></div>
      <label class="category-field">Danh mục<select v-model="filters.categoryId" aria-label="Danh mục" @change="changeCategory"><option value="">Tất cả danh mục</option><option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option></select></label>
      <template v-if="facets">
        <details open><summary>Lựa chọn hãng</summary><div class="brand-filter-grid"><button v-for="b in facets.brands" :key="b.id" :aria-label="b.name" :aria-pressed="array(filters.brandIds).includes(String(b.id))" :class="{selected:array(filters.brandIds).includes(String(b.id))}" @click="toggle('brandIds',b.id)"><BrandMark :brand="b" /><span v-if="array(filters.brandIds).includes(String(b.id))" class="brand-tick">✓</span></button></div><p v-if="!facets.brands.length" class="muted">Chưa có thương hiệu.</p></details>
        <details v-if="facets.total" open><summary>Mức giá</summary><FilterRange label="Giá" :min="facets.minPrice" :max="facets.maxPrice" :low="priceLow" :high="priceHigh" :step="10000" :format="money" @change="setPrice" /></details>
        <details open><summary>Tình trạng</summary><label class="facet-option"><input type="checkbox" :checked="filters.inStock === 'true'" @change="filters.inStock = $event.target.checked ? 'true' : ''" />Sẵn hàng <small>[{{ facets.inStockCount }}]</small></label></details>
        <p v-if="!route.query.categoryId" class="muted">Chọn danh mục để lọc theo thông số riêng.</p>
        <details v-for="spec in shownSpecs" :key="spec.name"><summary>{{ spec.name }}</summary><div class="facet-values"><label v-for="v in spec.values" :key="v.value" class="facet-option"><input type="checkbox" :checked="array(filters['spec.'+spec.name]).includes(v.value)" @change="toggle('spec.'+spec.name,v.value)" /><span>{{ v.value }}</span><small>[{{ v.count }}]</small></label></div></details>
        <button v-if="facets.specs.length > SPEC_LIMIT" type="button" class="text-button more-filters" @click="allSpecs = !allSpecs">{{ allSpecs ? "Thu gọn bộ lọc" : `Xem thêm ${facets.specs.length - SPEC_LIMIT} bộ lọc` }}</button>
        <small class="facet-count-note">Số lượng theo toàn bộ danh mục. Giá lọc là giá niêm yết.</small>
        <div class="filter-apply"><button type="button" :disabled="busy || !pending" @click="apply()">Áp dụng bộ lọc</button><small v-if="pending" role="status">Có thay đổi chưa áp dụng</small></div>
      </template>
    </aside>
    <section class="catalog-results">
      <form class="catalog-toolbar" @submit.prevent="apply()"><input v-model="filters.search" aria-label="Tìm sản phẩm" placeholder="Tìm trong danh mục…" /><select v-model="filters.sort" aria-label="Sắp xếp" @change="apply()"><option value="featured">Nổi bật</option><option value="newest">Mới nhất</option><option value="popular">Bán chạy</option><option value="price-asc">Giá tăng dần</option><option value="price-desc">Giá giảm dần</option></select><button :disabled="busy">Tìm</button></form>
      <div v-if="chips.length" class="active-filters"><button v-for="chip in chips" :key="chip.key+chip.value" @click="remove(chip)" :aria-label="`Bỏ ${chip.label}`">{{ chip.label }} ×</button></div>
      <TaskState :busy="busy" :error="error" /><button v-if="error" @click="load">Thử lại</button>
      <p v-if="result" class="muted" aria-live="polite">{{ result.totalElements }} sản phẩm</p>
      <div class="product-grid"><ProductCard v-for="p in result?.content" :key="p.id" :product="p" /></div>
      <div class="empty" v-if="result && !result.content.length"><p>Không tìm thấy sản phẩm phù hợp.</p><button @click="clear">Xóa bộ lọc</button></div>
      <div class="pagination" v-if="result?.totalPages > 1"><button :disabled="busy || result.first" @click="apply(result.number-1)">← Trước</button><span>{{ result.number+1 }} / {{ result.totalPages }}</span><button :disabled="busy || result.last" @click="apply(result.number+1)">Sau →</button></div>
    </section>
  </div>
</template>
