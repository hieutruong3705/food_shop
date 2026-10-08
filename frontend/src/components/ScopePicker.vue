<script setup>
import { computed, onUnmounted, provide, ref, watch } from 'vue';
import { BuildingStorefrontIcon, Squares2X2Icon, TagIcon, CubeIcon } from '@heroicons/vue/24/outline';
import ScopeGroupBrowser from './ScopeGroupBrowser.vue';
import ScopeProductList from './ScopeProductList.vue';
import { api } from '../services/api';
import { createScopeMeta } from '../lib/scopeMeta';
import { imageUrl, imageError, money, rateText } from '../lib/shop';

// "Applies to" editor shared by promotions and vouchers. The four modes never mix: the whole store,
// categories, brands or hand-picked products. In a category or brand mode each group is taken whole
// or by single products. `rate` (percent) is only used to show how the sale tag will look.
const props = defineProps({ modelValue: { type: Object, required: true }, kind: { type: String, required: true }, rate: { type: Number, default: 0 } });
const emit = defineEmits(['update:modelValue']);
const meta = createScopeMeta();
provide('scopeMeta', meta);

const scope = computed(() => ({
  scopeType: props.modelValue.scopeType || 'ALL',
  categoryIds: props.modelValue.categoryIds || [],
  brandIds: props.modelValue.brandIds || [],
  productIds: props.modelValue.productIds || [],
}));
const modes = [
  { value: 'ALL', title: 'Toàn bộ cửa hàng', detail: 'Mọi sản phẩm đang bán.', icon: BuildingStorefrontIcon },
  { value: 'CATEGORY', title: 'Theo danh mục', detail: 'Cả danh mục, hoặc chỉ một vài sản phẩm trong đó.', icon: Squares2X2Icon },
  { value: 'BRAND', title: 'Theo thương hiệu', detail: 'Cả hãng, hoặc chỉ một vài sản phẩm của hãng.', icon: TagIcon },
  { value: 'PRODUCT', title: 'Sản phẩm cụ thể', detail: 'Tìm và chọn từng sản phẩm bất kỳ.', icon: CubeIcon },
];
const preview = ref(null), busy = ref(false), error = ref('');
let timer, request = 0;

function select(value) {
  if (value === scope.value.scopeType) return;
  emit('update:modelValue', { ...props.modelValue, scopeType: value, categoryIds: [], brandIds: [], productIds: [] });
}
function patch(values) {
  emit('update:modelValue', { ...props.modelValue, ...values });
}
function groupChange(change) {
  const wholeKey = scope.value.scopeType === 'CATEGORY' ? 'categoryIds' : 'brandIds';
  patch({ ...(change.whole ? { [wholeKey]: change.whole } : {}), ...(change.products ? { productIds: change.products } : {}) });
}
const pickedProducts = computed(() => scope.value.productIds.map((id) => meta.state.products[id] || { id, name: '#' + id }));
watch(() => scope.value.scopeType === 'PRODUCT' && scope.value.productIds, (ids) => { if (ids) meta.hydrate('products', ids).catch(() => {}); }, { immediate: true, deep: true });

async function loadPreview(token) {
  try {
    const data = await api('/admin/offers/preview?kind=' + props.kind, { method: 'POST', body: JSON.stringify(scope.value) });
    if (token === request) preview.value = data;
  } catch (e) {
    if (token === request) error.value = e.message;
  } finally {
    if (token === request) busy.value = false;
  }
}
watch(scope, () => {
  clearTimeout(timer);
  const token = ++request;
  busy.value = true;
  error.value = '';
  timer = setTimeout(() => loadPreview(token), 300);
}, { immediate: true, deep: true });
onUnmounted(() => { request++; clearTimeout(timer); });

const salePrice = (price) => Math.round(price * (1 - props.rate / 100));
</script>

<template>
  <section class="scope-picker" aria-label="Phạm vi áp dụng">
    <h3>Áp dụng cho sản phẩm nào?</h3>
    <div class="scope-modes" role="group" aria-label="Chọn phạm vi áp dụng">
      <button v-for="mode in modes" :key="mode.value" type="button" :aria-pressed="scope.scopeType === mode.value" @click="select(mode.value)">
        <component :is="mode.icon" class="mode-icon" aria-hidden="true" />
        <span><strong>{{ mode.title }}</strong><small>{{ mode.detail }}</small></span>
      </button>
    </div>

    <ScopeGroupBrowser v-if="scope.scopeType === 'CATEGORY'" key="categories" kind="categories" :whole="scope.categoryIds" :products="scope.productIds" @change="groupChange" />
    <ScopeGroupBrowser v-else-if="scope.scopeType === 'BRAND'" key="brands" kind="brands" :whole="scope.brandIds" :products="scope.productIds" @change="groupChange" />
    <div v-else-if="scope.scopeType === 'PRODUCT'" class="scope-products">
      <ScopeProductList filters label="Sản phẩm áp dụng" :model-value="scope.productIds" @update:model-value="patch({ productIds: $event })" />
      <div class="tray" aria-label="Sản phẩm đã chọn">
        <strong>Đã chọn {{ pickedProducts.length }} sản phẩm</strong>
        <span v-for="p in pickedProducts" :key="p.id" class="tray-chip">
          <img :src="imageUrl(p.image)" alt="" @error="imageError" /><span class="tray-name">{{ p.name }}</span>
          <button type="button" :aria-label="`Bỏ ${p.name}`" @click="patch({ productIds: scope.productIds.filter((id) => id !== p.id) })">×</button>
        </span>
      </div>
    </div>
    <p v-else class="all-note">Ưu đãi này áp dụng cho mọi sản phẩm đang bán, kể cả sản phẩm thêm vào sau.</p>

    <div class="scope-preview" aria-live="polite" :aria-busy="busy">
      <p v-if="busy" class="muted">Đang xem trước phạm vi…</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <template v-else-if="preview">
        <template v-if="preview.valid">
          <div class="preview-head">
            <strong>Áp dụng cho {{ preview.total }} sản phẩm ({{ preview.inStock }} còn hàng)</strong>
            <small v-if="rate > 0 && kind === 'promotion'">Nhãn "Giảm {{ rateText(rate) }}%" sẽ hiện trên các sản phẩm này</small>
          </div>
          <p v-if="!preview.total" class="muted">Chưa có sản phẩm đang mở bán phù hợp với lựa chọn này.</p>
          <div class="scope-samples">
            <article v-for="item in preview.sample" :key="item.id">
              <span class="frame"><img :src="imageUrl(item.image)" :alt="item.name" @error="imageError" /><em v-if="rate > 0 && kind === 'promotion'" class="mini-tag">Giảm {{ rateText(rate) }}%</em></span>
              <span class="info">{{ item.name }}
                <small v-if="rate > 0 && kind === 'promotion'"><b>{{ money(salePrice(item.price)) }}</b> <del>{{ money(item.price) }}</del></small>
                <small v-else>{{ money(item.price) }}</small>
              </span>
            </article>
          </div>
          <p v-if="preview.total > preview.sample.length" class="muted more">… và {{ preview.total - preview.sample.length }} sản phẩm khác</p>
        </template>
        <p v-else class="warn">{{ preview.message }}</p>
      </template>
    </div>
  </section>
</template>

<style scoped>
.scope-picker { display: grid; gap: 14px; margin: 0; }
.scope-picker h3 { margin: 0; font-size: 16px; }
.scope-modes { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.scope-modes button { display: flex; align-items: flex-start; justify-content: flex-start; gap: 12px; padding: 14px; text-align: left; color: #2a5366; background: #fff; border: 1px solid #d7dfe3; border-radius: 12px; font-weight: 400; }
.scope-modes button:hover { border-color: #7fa5b6; }
.scope-modes button[aria-pressed="true"] { border-color: #0085c3; background: #e9f2f6; box-shadow: inset 0 0 0 1px #0085c3; }
.mode-icon { width: 26px; height: 26px; flex-shrink: 0; color: #007bb4; }
.scope-modes span { display: grid; gap: 4px; }
.scope-modes strong { font-size: 14px; }
.scope-modes small { font-size: 12px; color: #606d76; line-height: 1.4; }
.all-note { margin: 0; padding: 14px 16px; background: #f5f7f8; border-radius: 10px; font-size: 14px; color: #2a5366; }
.scope-products { display: grid; gap: 12px; }
.tray { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; padding: 10px 12px; background: #f5f7f8; border-radius: 10px; font-size: 13px; }
.tray-chip { display: inline-flex; align-items: center; gap: 6px; background: #fff; border: 1px solid #baced8; border-radius: 999px; padding: 3px 4px 3px 4px; max-width: 100%; }
.tray-chip img { width: 24px; height: 24px; object-fit: contain; border-radius: 50%; background: #f0f2f4; }
.tray-name { font-size: 12px; font-weight: 600; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tray-chip button { background: transparent; color: #7b848b; padding: 2px 8px; font-size: 15px; line-height: 1; gap: 0; }
.tray-chip button:hover { color: #b42a26; }
.scope-preview { border-radius: 12px; background: #f5f7f8; padding: 16px 18px; display: grid; gap: 12px; }
.scope-preview p { margin: 0; }
.preview-head { display: grid; gap: 2px; }
.preview-head small { color: #a5281f; font-size: 12px; font-weight: 600; }
.muted { color: #606d76; font-size: 13px; }
.more { text-align: center; }
.scope-samples { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.scope-samples article { display: grid; gap: 8px; font-size: 13px; background: #fff; border-radius: 10px; padding: 8px; min-width: 0; }
.frame { position: relative; display: block; background: #f4f6f7; border-radius: 8px; height: 90px; }
.frame img { width: 100%; height: 100%; object-fit: contain; }
.mini-tag { position: absolute; top: 6px; left: 6px; background: #df3c43; color: #fff; font-style: normal; font-size: 11px; font-weight: 700; border-radius: 4px; padding: 2px 6px; }
.info { display: grid; gap: 3px; line-height: 1.35; font-weight: 600; min-width: 0; overflow-wrap: anywhere; }
.info small { font-weight: 400; color: #606d76; font-size: 12px; }
.info small b { color: #d82f37; font-weight: 700; }
.error { color: #b42a26; }
.warn { background: #fff7e6; border: 1px solid #f2d59b; color: #7a4d0b; border-radius: 8px; padding: 10px 12px; font-size: 13px; }
@media (max-width: 980px) { .scope-modes { grid-template-columns: repeat(2, minmax(0, 1fr)); } .scope-samples { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 560px) { .scope-modes { grid-template-columns: 1fr; } }
</style>
