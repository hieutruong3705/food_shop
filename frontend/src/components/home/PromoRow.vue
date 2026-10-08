<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import ProductCard from '../ProductCard.vue';
import ShopIcon from '../ShopIcon.vue';
import { imageUrl, imageError, rateText } from '../../lib/shop';

// The running promotion with the highest rate and the products it discounts, counting down to its end.
const props = defineProps({ promotion: { type: Object, required: true } });
const now = ref(Date.now());
let timer;
const left = computed(() => Math.max(0, new Date(props.promotion.endDate).getTime() - now.value));
const parts = computed(() => {
  const total = Math.floor(left.value / 1000);
  return { days: Math.floor(total / 86400), hours: Math.floor(total / 3600) % 24, minutes: Math.floor(total / 60) % 60, seconds: total % 60 };
});
const two = (value) => String(value).padStart(2, '0');
onMounted(() => { timer = setInterval(() => { now.value = Date.now(); }, 1000); });
onUnmounted(() => clearInterval(timer));
</script>

<template>
  <section v-if="left > 0" class="home-block promo-row" aria-labelledby="promo-title">
    <img v-if="promotion.imageUrl" class="promo-banner" :src="imageUrl(promotion.imageUrl)" :alt="promotion.name" @error="imageError" />
    <div class="promo-head">
      <div>
        <span class="campaign-tag"><ShopIcon name="bolt" /> ƯU ĐÃI ĐANG DIỄN RA</span>
        <h2 id="promo-title">{{ promotion.name }}</h2><b class="promo-rate">Giảm {{ rateText(promotion.discountRate) }}%</b>
        <p v-if="promotion.description">{{ promotion.description }}</p>
        <small>{{ promotion.scopeDescription }}</small>
      </div>
      <div class="promo-clock" role="timer" aria-label="Thời gian còn lại của ưu đãi">
        <span>Kết thúc sau</span>
        <div><b v-if="parts.days">{{ parts.days }}<i>ngày</i></b><b>{{ two(parts.hours) }}<i>giờ</i></b><b>{{ two(parts.minutes) }}<i>phút</i></b><b>{{ two(parts.seconds) }}<i>giây</i></b></div>
      </div>
    </div>
    <div class="product-grid"><ProductCard v-for="p in promotion.products || []" :key="p.id" :product="p" /></div>
    <RouterLink class="promo-more" :to="{ path: '/products', query: { promotionId: promotion.id } }">Xem tất cả sản phẩm ưu đãi <ShopIcon name="arrow" /></RouterLink>
  </section>
</template>
