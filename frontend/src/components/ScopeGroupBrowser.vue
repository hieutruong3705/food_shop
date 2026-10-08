<script setup>
import { computed, inject, onMounted, onUnmounted, ref, watch } from 'vue';
import { api } from '../services/api';
import { imageUrl, imageError } from '../lib/shop';
import { categoryImage } from '../lib/presentation';
import ScopeProductList from './ScopeProductList.vue';

// Pick categories (or brands) and, inside each, either the whole group or single products.
// `whole` holds the ids of groups taken as a whole; `products` the single products picked in any group.
const props = defineProps({
  kind: { type: String, required: true }, // 'categories' | 'brands'
  whole: { type: Array, default: () => [] },
  products: { type: Array, default: () => [] },
});
const emit = defineEmits(['change']);
const meta = inject('scopeMeta');

const isCategory = computed(() => props.kind === 'categories');
const noun = computed(() => (isCategory.value ? 'danh mục' : 'thương hiệu'));
const groupKey = computed(() => (isCategory.value ? 'categoryId' : 'brandId'));
const known = computed(() => meta.state[props.kind]);

const query = ref(''), groups = ref([]), page = ref(0), last = ref(true), busy = ref(false), error = ref(''), activeId = ref(null);
let sequence = 0, timer, alive = true;

const active = computed(() => (activeId.value == null ? null : known.value[activeId.value] || null));
const isWhole = (id) => props.whole.includes(id);
const belongs = (productId, groupId) => meta.state.products[productId]?.[groupKey.value] === groupId;
const pickedIn = (id) => props.products.filter((productId) => belongs(productId, id)).length;

function picture(group) {
  if (group.image) return imageUrl(group.image);
  return isCategory.value ? categoryImage(group.name) : '';
}

async function load(append = false) {
  const token = ++sequence;
  busy.value = true;
  error.value = '';
  if (!append) page.value = 0;
  try {
    const data = await api(`/admin/options/${props.kind}?` + new URLSearchParams({ page: page.value, search: query.value.trim() }));
    if (!alive || token !== sequence) return;
    groups.value = append ? [...groups.value, ...data.content] : data.content;
    last.value = data.last;
    for (const row of data.content) meta.remember(props.kind, row);
    if (activeId.value == null) activeId.value = props.whole[0] ?? firstPartial() ?? groups.value[0]?.id ?? null;
  } catch (e) {
    if (token === sequence) error.value = e.message;
  } finally {
    if (token === sequence) busy.value = false;
  }
}
function firstPartial() {
  const productId = props.products.find((id) => meta.state.products[id]?.[groupKey.value] != null);
  return productId == null ? undefined : meta.state.products[productId][groupKey.value];
}
async function hydrate() {
  try {
    await Promise.all([meta.hydrate('products', props.products), meta.hydrate(props.kind, props.whole)]);
  } catch (e) {
    error.value = e.message;
  }
}

function setWhole(on) {
  const id = active.value.id;
  if (on) emit('change', { whole: [...props.whole.filter((x) => x !== id), id], products: props.products.filter((productId) => !belongs(productId, id)) });
  else emit('change', { whole: props.whole.filter((x) => x !== id) });
}
function setProducts(ids) {
  emit('change', { products: ids });
}

// One chip per selected group: "Laptop · Toàn bộ" or "Dell · 3 sản phẩm".
const summary = computed(() => {
  const chips = props.whole.map((id) => ({ id, whole: true, count: 0 }));
  const partial = new Map();
  for (const productId of props.products) {
    const id = meta.state.products[productId]?.[groupKey.value];
    if (id != null && !props.whole.includes(id)) partial.set(id, (partial.get(id) || 0) + 1);
  }
  for (const [id, count] of partial) chips.push({ id, whole: false, count });
  return chips.map((chip) => ({ ...chip, name: known.value[chip.id]?.name || '#' + chip.id }));
});
function removeChip(chip) {
  if (chip.whole) emit('change', { whole: props.whole.filter((id) => id !== chip.id) });
  else emit('change', { products: props.products.filter((productId) => !belongs(productId, chip.id)) });
}

watch(query, () => { clearTimeout(timer); timer = setTimeout(() => load(), 250); });
watch(() => [props.whole, props.products], hydrate, { deep: true });
onMounted(async () => { await hydrate(); load(); });
onUnmounted(() => { alive = false; sequence++; clearTimeout(timer); });
</script>

<template>
  <div class="browser">
    <div class="panes">
      <section class="groups" :aria-label="`Chọn ${noun}`">
        <input v-model="query" type="search" :aria-label="`Tìm ${noun}`" :placeholder="`Tìm ${noun}…`" />
        <div class="group-list">
          <button v-for="group in groups" :key="group.id" type="button" class="group" :class="{ current: activeId === group.id }"
            :aria-pressed="activeId === group.id" @click="activeId = group.id">
            <span class="thumb"><img v-if="picture(group)" :src="picture(group)" alt="" @error="imageError" /><b v-else aria-hidden="true">{{ group.name.slice(0, 1) }}</b></span>
            <span class="text"><strong>{{ group.name }}</strong><small>{{ group.productCount }} sản phẩm</small></span>
            <span v-if="isWhole(group.id)" class="chip solid">Toàn bộ</span>
            <span v-else-if="pickedIn(group.id)" class="chip">{{ pickedIn(group.id) }} sản phẩm</span>
          </button>
          <p v-if="!busy && !groups.length && !error" class="hint">Không có {{ noun }} phù hợp.</p>
          <p v-if="busy" class="hint" role="status">Đang tải…</p>
          <button v-if="!last" type="button" class="more" :disabled="busy" @click="page++; load(true)">Xem thêm {{ noun }}</button>
        </div>
        <p v-if="error" class="fail" role="alert">{{ error }} <button type="button" class="more" @click="load()">Thử lại</button></p>
      </section>

      <section class="detail" :aria-label="active ? `Áp dụng cho ${noun} ${active.name}` : `Chi tiết ${noun}`">
        <div v-if="!active" class="placeholder">
          <strong>Chọn một {{ noun }} ở bên trái</strong>
          <span>Sau đó áp dụng cho cả {{ noun }} hoặc chỉ chọn một vài sản phẩm bên trong.</span>
        </div>
        <template v-else>
          <header class="head">
            <span class="thumb big"><img v-if="picture(active)" :src="picture(active)" alt="" @error="imageError" /><b v-else aria-hidden="true">{{ active.name.slice(0, 1) }}</b></span>
            <div class="title"><h4>{{ active.name }}</h4><small>{{ active.productCount ?? 0 }} sản phẩm đang bán</small></div>
            <div class="mode" role="group" aria-label="Cách áp dụng">
              <button type="button" :aria-pressed="isWhole(active.id)" @click="setWhole(true)">Toàn bộ {{ noun }}</button>
              <button type="button" :aria-pressed="!isWhole(active.id)" @click="setWhole(false)">Chọn từng sản phẩm<span v-if="pickedIn(active.id)" class="count">{{ pickedIn(active.id) }}</span></button>
            </div>
          </header>
          <p v-if="isWhole(active.id)" class="note">Mọi sản phẩm của {{ noun }} <strong>{{ active.name }}</strong> đều được áp dụng, kể cả sản phẩm thêm vào sau này.</p>
          <ScopeProductList :key="kind + active.id" :model-value="products" :locked="isWhole(active.id)"
            :category-id="isCategory ? active.id : null" :brand-id="isCategory ? null : active.id"
            :label="`Sản phẩm của ${noun} ${active.name}`" @update:model-value="setProducts" />
        </template>
      </section>
    </div>

    <div class="tray" aria-label="Đã chọn">
      <strong>Đã chọn</strong>
      <span v-for="chip in summary" :key="(chip.whole ? 'w' : 'p') + chip.id" class="tray-chip" :class="{ solid: chip.whole }">
        <button type="button" class="open" @click="activeId = chip.id">{{ chip.name }} · {{ chip.whole ? 'toàn bộ' : chip.count + ' sản phẩm' }}</button>
        <button type="button" class="drop" :aria-label="`Bỏ ${chip.name}`" @click="removeChip(chip)">×</button>
      </span>
      <small v-if="!summary.length">Chưa chọn {{ noun }} hoặc sản phẩm nào.</small>
    </div>
  </div>
</template>

<style scoped>
.browser { display: grid; gap: 12px; }
.panes { display: grid; grid-template-columns: minmax(230px, 300px) minmax(0, 1fr); gap: 14px; align-items: start; }
.groups, .detail { border: 1px solid #d7dfe3; border-radius: 12px; background: #fff; padding: 12px; min-width: 0; }
.groups { display: grid; gap: 10px; }
.groups input { padding: 10px 12px; }
.group-list { display: grid; gap: 6px; max-height: 470px; overflow: auto; align-content: start; }
.group { display: grid; grid-template-columns: 40px minmax(0, 1fr) auto; gap: 10px; align-items: center; text-align: left; background: #fff; color: #2a5366; border: 1px solid transparent; border-radius: 10px; padding: 8px; font-weight: 400; }
.group:hover { background: #f4f7f9; }
.group.current { background: #e9f2f6; border-color: #0085c3; }
.thumb { width: 40px; height: 40px; border-radius: 8px; background: #f0f2f4; display: grid; place-items: center; overflow: hidden; flex-shrink: 0; }
.thumb img { width: 100%; height: 100%; object-fit: contain; }
.thumb b { font-size: 17px; color: #007bb4; }
.thumb.big { width: 52px; height: 52px; }
.text { display: grid; min-width: 0; }
.text strong { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.text small { font-size: 12px; color: #7b848b; }
.chip { font-size: 11px; font-weight: 600; color: #007bb4; background: #e2eef3; border-radius: 999px; padding: 3px 8px; white-space: nowrap; }
.chip.solid { background: #0085c3; color: #fff; }
.hint { color: #606d76; font-size: 13px; text-align: center; margin: 10px 0; }
.fail { color: #b42a26; font-size: 13px; }
.more { background: #eef2f4; color: #2a5366; padding: 8px 14px; font-size: 13px; justify-self: center; }
.detail { display: grid; gap: 12px; }
.placeholder { display: grid; gap: 6px; text-align: center; color: #606d76; padding: 60px 20px; font-size: 14px; }
.placeholder strong { color: #2a5366; font-size: 16px; }
.head { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.title { flex: 1 1 160px; min-width: 0; }
.title h4 { margin: 0; font-size: 17px; }
.title small { color: #606d76; font-size: 13px; }
.mode { display: inline-flex; background: #eef2f4; border-radius: 10px; padding: 3px; gap: 2px; }
.mode button { background: transparent; color: #3d515a; padding: 8px 14px; font-size: 13px; font-weight: 600; border-radius: 8px; gap: 8px; }
.mode button[aria-pressed="true"] { background: #0085c3; color: #fff; }
.count { background: #fff; color: #007bb4; border-radius: 999px; font-size: 11px; padding: 1px 7px; }
.note { margin: 0; background: #e9f2f6; border-radius: 8px; padding: 10px 12px; font-size: 13px; color: #2a5366; }
.tray { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; padding: 10px 12px; background: #f5f7f8; border-radius: 10px; font-size: 13px; }
.tray small { color: #606d76; }
.tray-chip { display: inline-flex; align-items: center; background: #fff; border: 1px solid #baced8; border-radius: 999px; overflow: hidden; }
.tray-chip.solid { background: #e2eef3; border-color: #0085c3; }
.tray-chip button { background: transparent; color: #2a5366; padding: 6px 10px; font-size: 12px; font-weight: 600; gap: 0; }
.tray-chip .drop { padding: 6px 10px 6px 4px; font-size: 15px; line-height: 1; color: #7b848b; }
.tray-chip .drop:hover { color: #b42a26; }
@media (max-width: 820px) { .panes { grid-template-columns: 1fr; } .group-list { max-height: 260px; } }
</style>
