<script setup>
import { onMounted, ref } from 'vue';
import { api } from '../services/api';
import { useTask, imageUrl, imageError, money, date } from '../lib/shop';
import TaskState from '../components/TaskState.vue';
import ShopIcon from '../components/ShopIcon.vue';
const banners = ref([]), vouchers = ref([]), copied = ref('');
const { busy, error, run } = useTask();
async function copy(code){try{await navigator.clipboard.writeText(code);copied.value='Đã sao chép mã '+code;}catch{copied.value='Bạn có thể sao chép mã hiển thị trên thẻ.';}}
onMounted(() => run(async () => {
  const [bannerData, offerData] = await Promise.all([api('/banners?placement=VOUCHER_PAGE'), api('/offers')]);
  banners.value = Array.isArray(bannerData) ? bannerData : [];
  vouchers.value = Array.isArray(offerData?.vouchers) ? offerData.vouchers : [];
}));
</script>
<template>
  <div class="page-heading voucher-heading">
    <span class="eyebrow">ƯU ĐÃI TECHSHOP</span>
    <h1>Mã giảm giá</h1>
    <p>Lưu lại voucher phù hợp trước khi hoàn tất đơn hàng.</p>
  </div>
  <TaskState :busy="busy" :error="error" />
  <section v-if="banners.length" class="voucher-banners" aria-label="Banner giảm giá">
    <a v-for="banner in banners" :key="banner.id" :href="banner.link || '/products'">
      <img v-if="banner.imageUrl" :src="imageUrl(banner.imageUrl)" :alt="banner.name" @error="imageError" />
      <span v-else class="voucher-banner-empty"><ShopIcon name="bolt" />{{ banner.name }}</span>
    </a>
  </section>
  <section class="voucher-list" aria-labelledby="voucher-list-title">
    <div class="retail-section-title"><h2 id="voucher-list-title">Danh sách mã giảm giá</h2><RouterLink to="/products">Tiếp tục mua sắm <ShopIcon name="arrow" /></RouterLink></div>
    <div v-if="vouchers.length" class="voucher-grid">
      <article v-for="voucher in vouchers" :key="voucher.code" class="voucher-item">
        <div v-if="voucher.imageUrl" class="voucher-media"><img :src="imageUrl(voucher.imageUrl)" :alt="voucher.code" @error="imageError" /></div>
        <strong>Giảm {{ voucher.discount }}%</strong>
        <button class="voucher-code" :aria-label="`Sao chép mã ${voucher.code}`" @click="copy(voucher.code)">{{ voucher.code }} <span>⧉</span></button>
        <p>Đơn từ {{ money(voucher.minimumOrder) }}</p>
        <p v-if="voucher.maximumDiscount">Giảm tối đa {{ money(voucher.maximumDiscount) }}</p>
        <p v-if="voucher.endDate">Hạn dùng: {{ date(voucher.endDate) }}</p>
        <small v-if="voucher.scopeDescription">{{ voucher.scopeDescription }}</small>
      </article>
    </div>
    <p v-if="!busy && !error && !vouchers.length" class="empty">Chưa có mã giảm giá còn hiệu lực. Hãy quay lại sau nhé.</p>
    <p v-if="copied" role="status" class="notice success">{{ copied }}</p>
  </section>
</template>
