<script setup>
import { ref, watch, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../services/api';
import { money } from '../lib/shop';
import ProductCard from '../components/ProductCard.vue';
import TaskState from '../components/TaskState.vue';
import BrandMark from '../components/BrandMark.vue';
import RangeSlider from '../components/RangeSlider.vue';
const route = useRoute(), router = useRouter();
const result = ref(null), categories = ref([]), facets = ref(null), filters = ref({}), busy = ref(false), error = ref('');
const filtersOpen = ref(false);
const warrantyMin=ref(0),warrantyMax=ref(0);
const hiddenSpecs=['phiên bản','kết nối','bảo hành','version','connection','warranty'];
let requestId = 0;
const array = value => value == null ? [] : Array.isArray(value) ? value : [value];
const categoryName = computed(() => categories.value.find(c => String(c.id) === String(route.query.categoryId))?.name);
function queryString(query) { const params = new URLSearchParams(); for (const [k,v] of Object.entries(query)) for(const item of array(v)) if(item !== '') params.append(k,item); return params.toString(); }
async function load() {
  const obsolete=Object.keys(route.query).filter(key=>key.startsWith('spec.')&&hiddenSpecs.includes(key.slice(5).trim().toLowerCase()));
  if(obsolete.length){const query={...route.query};for(const key of obsolete)delete query[key];await router.replace({query});return;}
  const id = ++requestId;
  filters.value = {...route.query, categoryId:route.query.categoryId || '', sort:route.query.sort || 'newest', brandIds: array(route.query.brandIds || route.query.brandId).map(String)};
  delete filters.value.brandId;
  busy.value = true; error.value = ''; facets.value = null;
  try {
    const [p,f] = await Promise.all([api('/products?' + queryString(route.query)), api('/catalog/facets?' + queryString({categoryId:route.query.categoryId}))]);
    if(id === requestId) { result.value = p; facets.value = f;warrantyMin.value=Number(route.query.minWarranty??f.minWarranty);warrantyMax.value=Number(route.query.maxWarranty??f.maxWarranty); }
  } catch(e) { if(id === requestId) { error.value = e.message; result.value = null; } }
  finally { if(id === requestId) busy.value = false; }
}
function apply(page = 0) {
  const q = {...filters.value, page}; delete q.brandId;
  for(const k of Object.keys(q)) if(q[k] == null || q[k] === '' || (Array.isArray(q[k]) && !q[k].length) || q[k] === false) delete q[k];
  router.push({query:q});
}
function changeCategory() { const categoryId = filters.value.categoryId; filters.value = {categoryId, sort:filters.value.sort || 'newest'}; apply(); }
function toggle(key,value) { const list = array(filters.value[key]).map(String); filters.value[key] = list.includes(String(value)) ? list.filter(x => x !== String(value)) : [...list,String(value)]; apply(); }
function priceRange(min,max) { filters.value.minPrice = min; filters.value.maxPrice = max; apply(); }
function applyWarranty(){const f=facets.value;filters.value.minWarranty=warrantyMin.value>f.minWarranty?warrantyMin.value:'';filters.value.maxWarranty=warrantyMax.value<f.maxWarranty?warrantyMax.value:'';apply();}
function clear() { filters.value = {categoryId:route.query.categoryId}; apply(); }
const chips = computed(() => Object.entries(route.query).filter(([key]) => key.startsWith('spec.') || ['inStock','brandIds','brandId','minPrice','maxPrice','promotionId','minWarranty','maxWarranty'].includes(key)).flatMap(([key,v]) => array(v).map(value => ({key,value,label:key.startsWith('spec.') ? `${key.slice(5)}: ${value}` : key.endsWith('Warranty')?`Bảo hành ${key==='minWarranty'?'từ':'đến'} ${value} tháng`:key === 'promotionId' ? 'Khuyến mãi đang chọn' : key === 'inStock' ? 'Sẵn hàng' : key.includes('brand') ? facets.value?.brands.find(b => String(b.id) === value)?.name || 'Thương hiệu' : `${key === 'minPrice' ? 'Từ' : 'Đến'} ${money(value)}`}))));
function remove(chip) { const key = chip.key === 'brandId' ? 'brandIds' : chip.key; filters.value[key] = array(filters.value[key]).filter(v => String(v) !== String(chip.value)); apply(); }
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
        <details open><summary>Mức giá</summary><form class="price-filter" @submit.prevent="apply()"><label>Tối đa: {{ money(filters.maxPrice ?? facets.maxPrice) }}<input type="range" aria-label="Giá tối đa" min="0" :max="Math.max(facets.maxPrice,1)" step="1" :value="filters.maxPrice ?? facets.maxPrice" @input="filters.maxPrice = $event.target.value" /></label><div><input v-model="filters.minPrice" type="number" min="0" aria-label="Giá từ" placeholder="Giá từ" /><input v-model="filters.maxPrice" type="number" min="0" aria-label="Giá đến" placeholder="Giá đến" /></div><button :disabled="busy">Áp dụng giá</button></form><div class="price-presets"><button @click="priceRange('',Math.round(facets.maxPrice/3))">Đến {{ money(Math.round(facets.maxPrice/3)) }}</button><button @click="priceRange(Math.round(facets.maxPrice/3),Math.round(facets.maxPrice*2/3))">{{ money(Math.round(facets.maxPrice/3)) }} – {{ money(Math.round(facets.maxPrice*2/3)) }}</button><button @click="priceRange(Math.round(facets.maxPrice*2/3),'')">Trên {{ money(Math.round(facets.maxPrice*2/3)) }}</button></div></details>
        <details open><summary>Tình trạng</summary><label class="facet-option"><input type="checkbox" :checked="filters.inStock === 'true'" @change="filters.inStock = $event.target.checked ? 'true' : ''; apply()" />Sẵn hàng <small>[{{ facets.inStockCount }}]</small></label></details>
        <p v-if="!route.query.categoryId" class="muted">Chọn danh mục để lọc theo thông số riêng.</p>
        <details v-if="facets.total" open><summary>Bảo hành</summary><RangeSlider v-model:low="warrantyMin" v-model:high="warrantyMax" :min="facets.minWarranty" :max="facets.maxWarranty" label="Bảo hành" unit="tháng" @change="applyWarranty" /></details>
        <details v-for="spec in facets.specs" :key="spec.name" open><summary>{{ spec.name }}</summary><div class="facet-values"><label v-for="v in spec.values" :key="v.value" class="facet-option"><input type="checkbox" :checked="array(filters['spec.'+spec.name]).includes(v.value)" @change="toggle('spec.'+spec.name,v.value)" /><span>{{ v.value }}</span><small>[{{ v.count }}]</small></label></div></details>
        <small class="facet-count-note">Số lượng theo toàn bộ danh mục. Giá lọc là giá niêm yết.</small>
      </template>
    </aside>
    <section class="catalog-results">
      <form class="catalog-toolbar" @submit.prevent="apply()"><input v-model="filters.search" aria-label="Tìm sản phẩm" placeholder="Tìm trong danh mục…" /><select v-model="filters.sort" aria-label="Sắp xếp" @change="apply()"><option value="newest">Mới nhất</option><option value="popular">Bán chạy</option><option value="price-asc">Giá tăng dần</option><option value="price-desc">Giá giảm dần</option></select><button :disabled="busy">Áp dụng</button></form>
      <div v-if="chips.length" class="active-filters"><button v-for="chip in chips" :key="chip.key+chip.value" @click="remove(chip)" :aria-label="`Bỏ ${chip.label}`">{{ chip.label }} ×</button></div>
      <TaskState :busy="busy" :error="error" /><button v-if="error" @click="load">Thử lại</button>
      <p v-if="result" class="muted" aria-live="polite">{{ result.totalElements }} sản phẩm</p>
      <div class="product-grid"><ProductCard v-for="p in result?.content" :key="p.id" :product="p" /></div>
      <div class="empty" v-if="result && !result.content.length"><p>Không tìm thấy sản phẩm phù hợp.</p><button @click="clear">Xóa bộ lọc</button></div>
      <div class="pagination" v-if="result?.totalPages > 1"><button :disabled="busy || result.first" @click="apply(result.number-1)">← Trước</button><span>{{ result.number+1 }} / {{ result.totalPages }}</span><button :disabled="busy || result.last" @click="apply(result.number+1)">Sau →</button></div>
    </section>
  </div>
</template>
