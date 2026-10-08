<script setup>
import { computed, inject, onMounted, onUnmounted, ref, watch } from 'vue';
import { api } from '../services/api';
import { imageUrl, imageError, money } from '../lib/shop';

// A searchable grid of products with pictures. It is used inside a category or brand (the group is
// fixed by categoryId / brandId) and on its own for hand-picked products (filters shows the group menus).
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  categoryId: { type: [Number, String], default: null },
  brandId: { type: [Number, String], default: null },
  locked: { type: Boolean, default: false },
  filters: { type: Boolean, default: false },
  label: { type: String, default: 'Sản phẩm' },
});
const emit = defineEmits(['update:modelValue']);
const meta = inject('scopeMeta');

const query = ref(''), rows = ref([]), page = ref(0), last = ref(true), total = ref(0), busy = ref(false), error = ref('');
const pickCategory = ref(''), pickBrand = ref('');
const categories = ref([]), brands = ref([]);
let sequence = 0, timer, alive = true;

const categoryFilter = computed(() => props.categoryId ?? pickCategory.value);
const brandFilter = computed(() => props.brandId ?? pickBrand.value);
const chosen = computed(() => new Set(props.modelValue));
const chosenHere = computed(() => rows.value.filter((row) => chosen.value.has(row.id)).length);

function params(index) {
  const query_ = { page: index, search: query.value.trim() };
  if (categoryFilter.value) query_.categoryId = categoryFilter.value;
  if (brandFilter.value) query_.brandId = brandFilter.value;
  return new URLSearchParams(query_);
}
async function load(append = false) {
  const token = ++sequence;
  busy.value = true;
  error.value = '';
  if (!append) page.value = 0;
  try {
    const data = await api('/admin/options/products?' + params(page.value));
    if (!alive || token !== sequence) return;
    rows.value = append ? [...rows.value, ...data.content] : data.content;
    last.value = data.last;
    total.value = data.totalElements;
    for (const row of data.content) meta.remember('products', row);
  } catch (e) {
    if (token === sequence) error.value = e.message;
  } finally {
    if (token === sequence) busy.value = false;
  }
}
async function loadMenus() {
  const all = async (resource) => {
    const list = [];
    let index = 0, data;
    do {
      data = await api(`/admin/${resource}?page=${index++}&size=100`);
      list.push(...data.content);
    } while (!data.last && alive);
    return list;
  };
  try {
    [categories.value, brands.value] = await Promise.all([all('categories'), all('brands')]);
  } catch (e) {
    error.value = e.message;
  }
}
function toggle(id) {
  if (props.locked) return;
  emit('update:modelValue', chosen.value.has(id) ? props.modelValue.filter((x) => x !== id) : [...props.modelValue, id]);
}
function selectShown() {
  emit('update:modelValue', [...new Set([...props.modelValue, ...rows.value.map((row) => row.id)])]);
}
function clearShown() {
  const shown = new Set(rows.value.map((row) => row.id));
  emit('update:modelValue', props.modelValue.filter((id) => !shown.has(id)));
}
const stock = (row) => (row.quantity > 0 ? 'Còn ' + row.quantity : 'Hết hàng');

watch(query, () => { clearTimeout(timer); timer = setTimeout(() => load(), 250); });
watch([categoryFilter, brandFilter], () => load());
onMounted(() => { load(); if (props.filters) loadMenus(); });
onUnmounted(() => { alive = false; sequence++; clearTimeout(timer); });
</script>

<template>
  <section class="product-list" :aria-label="label" :aria-busy="busy">
    <div class="list-tools">
      <input v-model="query" type="search" :aria-label="`Tìm ${label.toLowerCase()}`" placeholder="Tìm theo tên hoặc SKU…" />
      <select v-if="filters" v-model="pickCategory" aria-label="Lọc theo danh mục">
        <option value="">Mọi danh mục</option>
        <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <select v-if="filters" v-model="pickBrand" aria-label="Lọc theo thương hiệu">
        <option value="">Mọi thương hiệu</option>
        <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option>
      </select>
    </div>
    <div v-if="!locked" class="list-bar">
      <span><strong>{{ total }}</strong> sản phẩm<template v-if="chosenHere"> · đã chọn {{ chosenHere }} trong danh sách này</template></span>
      <span class="list-actions">
        <button type="button" class="link" :disabled="!rows.length" @click="selectShown">Chọn tất cả đang hiện</button>
        <button type="button" class="link" :disabled="!chosenHere" @click="clearShown">Bỏ chọn</button>
      </span>
    </div>
    <div class="grid" role="group" :aria-label="label">
      <label v-for="row in rows" :key="row.id" class="pick" :class="{ on: locked || chosen.has(row.id), off: !row.active }">
        <input type="checkbox" class="check" :checked="locked || chosen.has(row.id)" :disabled="locked" :aria-label="row.name" @change="toggle(row.id)" />
        <img :src="imageUrl(row.image)" alt="" loading="lazy" @error="imageError" />
        <span class="body">
          <span class="name">{{ row.name }}</span>
          <span class="sub">{{ row.sku }}<template v-if="filters"> · {{ row.brand }} · {{ row.category }}</template></span>
          <span class="cost">{{ money(row.price) }}<em :class="{ out: !(row.quantity > 0) }">{{ stock(row) }}</em><em v-if="!row.active" class="out">Đang ẩn</em></span>
        </span>
      </label>
    </div>
    <p v-if="!busy && !rows.length && !error" class="empty">Không có sản phẩm phù hợp.</p>
    <p v-if="busy" class="empty" role="status">Đang tải…</p>
    <p v-if="error" class="fail" role="alert">{{ error }} <button type="button" class="link" @click="load()">Thử lại</button></p>
    <button v-if="!last" type="button" class="more" :disabled="busy" @click="page++; load(true)">Xem thêm sản phẩm</button>
  </section>
</template>

<style scoped>
.product-list { display: grid; gap: 10px; min-width: 0; }
.list-tools { display: flex; flex-wrap: wrap; gap: 8px; }
.list-tools input { flex: 1 1 200px; min-width: 0; padding: 10px 12px; }
.list-tools select { flex: 0 1 180px; padding: 10px; }
.list-bar { display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 13px; color: #606d76; }
.list-actions { display: flex; gap: 4px; }
.link { background: none; color: #007bb4; padding: 6px 8px; font-size: 13px; font-weight: 600; gap: 0; }
.link:disabled { color: #9aa5ad; cursor: default; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(260px, 100%), 1fr)); gap: 8px; max-height: 420px; overflow: auto; padding: 2px; }
.pick { position: relative; display: grid; grid-template-columns: 64px minmax(0, 1fr); gap: 12px; align-items: center; padding: 10px 12px 10px 10px; border: 1px solid #d7dfe3; border-radius: 10px; background: #fff; cursor: pointer; font-weight: 400; }
.pick:hover { border-color: #7fa5b6; }
.pick.on { border-color: #0085c3; background: #f0f6f9; box-shadow: inset 0 0 0 1px #0085c3; }
.pick.off { opacity: .7; }
.check { position: absolute; top: 8px; right: 8px; width: 18px; height: 18px; padding: 0; accent-color: #0085c3; }
.pick img { width: 64px; height: 64px; object-fit: contain; background: #f4f6f7; border-radius: 8px; }
.body { display: grid; gap: 3px; min-width: 0; padding-right: 20px; }
.name { font-size: 13px; font-weight: 600; line-height: 1.35; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.sub { font-size: 12px; color: #7b848b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cost { font-size: 13px; font-weight: 700; color: #2a5366; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.cost em { font-style: normal; font-size: 11px; font-weight: 600; color: #007bb4; background: #e2eef3; border-radius: 999px; padding: 1px 7px; }
.cost em.out { color: #a5541a; background: #fbeadb; }
.empty { color: #606d76; font-size: 13px; text-align: center; padding: 18px 0; margin: 0; }
.fail { color: #b42a26; font-size: 13px; }
.more { justify-self: center; background: #eef2f4; color: #2a5366; padding: 9px 18px; font-size: 13px; }
</style>
