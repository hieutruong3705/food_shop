<script setup>
import { computed, ref, watch, onUnmounted } from 'vue';
import SearchMultiSelect from './SearchMultiSelect.vue';
import { api } from '../services/api';
import { imageUrl, imageError, money } from '../lib/shop';

const props = defineProps({ modelValue: { type: Object, required: true }, kind: { type: String, required: true } });
const emit = defineEmits(['update:modelValue']);
const scope = computed(() => ({
  scopeType: props.modelValue.scopeType || 'ALL',
  categoryIds: props.modelValue.categoryIds || [],
  brandIds: props.modelValue.brandIds || [],
  productIds: props.modelValue.productIds || [],
}));
const modes = [
  { value: 'ALL', title: 'Toàn bộ cửa hàng', detail: 'Áp dụng cho tất cả sản phẩm.' },
  { value: 'CATALOG', title: 'Theo danh mục và thương hiệu', detail: 'Kết hợp danh mục với các hãng phù hợp.' },
  { value: 'PRODUCT', title: 'Sản phẩm cụ thể', detail: 'Tự chọn từng sản phẩm.' },
];
const preview = ref(null), busy = ref(false), error = ref('');
let timer, request = 0;
function select(value) {
  emit('update:modelValue', { ...props.modelValue, scopeType: value, categoryIds: [], brandIds: [], productIds: [] });
}
function change(key, ids) {
  emit('update:modelValue', { ...props.modelValue, [key]: ids });
}
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
</script>
<template>
  <section class="scope-picker" aria-label="Phạm vi áp dụng">
    <h3>Phạm vi áp dụng</h3>
    <div class="scope-modes" role="group" aria-label="Chọn phạm vi áp dụng">
      <button v-for="mode in modes" :key="mode.value" type="button"
        :aria-pressed="scope.scopeType === mode.value" @click="select(mode.value)">
        <strong>{{ mode.title }}</strong><small>{{ mode.detail }}</small>
      </button>
    </div>
    <div v-if="scope.scopeType === 'CATALOG'" class="scope-catalog">
      <SearchMultiSelect :model-value="scope.categoryIds" resource="categories" label="Danh mục áp dụng" @update:model-value="change('categoryIds', $event)" />
      <SearchMultiSelect :model-value="scope.brandIds" resource="brands" label="Thương hiệu áp dụng" :category-ids="scope.categoryIds" @update:model-value="change('brandIds', $event)" />
      <p class="muted">Chọn cả danh mục và thương hiệu: chỉ áp dụng sản phẩm thỏa cả hai nhóm. Để trống một nhóm để không giới hạn nhóm đó.</p>
    </div>
    <SearchMultiSelect v-else-if="scope.scopeType === 'PRODUCT'" :model-value="scope.productIds" resource="products" label="Sản phẩm áp dụng" @update:model-value="change('productIds', $event)" />
    <div class="scope-preview" aria-live="polite" :aria-busy="busy">
      <p v-if="busy">Đang xem trước phạm vi…</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <template v-else-if="preview">
        <template v-if="preview.valid">
          <strong>Áp dụng cho {{ preview.total }} sản phẩm ({{ preview.inStock }} còn hàng)</strong>
          <p v-if="!preview.total" class="muted">Chưa có sản phẩm đang mở bán phù hợp với lựa chọn này.</p>
          <div class="scope-samples">
            <article v-for="item in preview.sample" :key="item.id">
              <img :src="imageUrl(item.image)" :alt="item.name" @error="imageError" />
              <span>{{ item.name }}<small>{{ money(item.price) }}</small></span>
            </article>
          </div>
        </template>
        <p v-else class="error">{{ preview.message }}</p>
      </template>
    </div>
  </section>
</template>
<style scoped>
.scope-picker { margin: 1.5rem 0; }
.scope-modes { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 20px; }
.scope-modes button { padding: 18px; text-align: left; color: var(--ink, #203b36); background: white; border: 1px solid #d7e3df; border-radius: 10px; }
.scope-modes button[aria-pressed="true"] { border-color: #00876d; background: #e9f6f0; box-shadow: inset 0 0 0 1px #00876d; }
.scope-modes small, .scope-samples small { display: block; margin-top: 6px; }
.scope-catalog { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.scope-catalog > p { grid-column: 1 / -1; }
.scope-preview { border-radius: 10px; background: #f5f8f7; padding: 18px; margin-top: 16px; }
.scope-samples { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 12px; margin-top: 14px; }
.scope-samples article { display: flex; gap: 8px; align-items: center; font-size: 13px; }
.scope-samples img { width: 48px; height: 48px; object-fit: contain; background: white; border-radius: 6px; }
.error { color: #b42a26; }
@media(max-width: 700px) { .scope-modes,.scope-catalog { grid-template-columns: 1fr; } .scope-samples { grid-template-columns: 1fr 1fr; } }
</style>
