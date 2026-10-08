<script setup>
import { computed } from "vue";
import { money, imageUrl, imageError, saleRate, rateText } from "../lib/shop";
import ShopIcon from "./ShopIcon.vue";
const props = defineProps({ product: Object });
const onSale = computed(() => props.product.discountedPrice < props.product.price);
const rate = computed(() => rateText(saleRate(props.product)));
// Stars are shown only once the product has been reviewed; an empty row would read as zero stars.
const rated = computed(() => (props.product.ratingCount || 0) > 0);
const stars = computed(() => {
  const score = Math.round(props.product.ratingAverage || 0);
  return [1, 2, 3, 4, 5].map(i => (i <= score ? 'full' : 'blank'));
});
const warranty = computed(() => Number(props.product.warrantyMonths) || 0);
</script>
<template>
  <RouterLink :to="`/products/${product.id}`" class="product-card">
    <div class="product-image">
      <span v-if="onSale" class="badge" :title="product.promotionName || undefined"
        >Giảm {{ rate }}%</span
      ><img
        :src="imageUrl(product.image)"
        :alt="product.name"
        loading="lazy"
        @error="imageError"
      />
    </div>
    <div class="product-copy">
      <small>{{ product.brand || "TechShop" }} · {{ product.category }}</small>
      <h3>{{ product.name }}</h3>
      <p>{{ product.shortDescription }}</p>
      <div class="card-rating" :class="{ blank: !rated }" :aria-label="rated ? `Đánh giá ${product.ratingAverage} trên 5 sao, ${product.ratingCount} lượt` : 'Chưa có đánh giá'">
        <template v-if="rated"><span aria-hidden="true"><i v-for="(s, i) in stars" :key="i" :class="s">★</i></span>
        <small>{{ product.ratingAverage }} ({{ product.ratingCount }})</small></template>
        <small v-else class="muted">Chưa có đánh giá</small>
      </div>
      <div class="price-block">
        <strong>{{ money(product.discountedPrice) }}</strong
        ><del v-if="onSale">{{
          money(product.price)
        }}</del
        ><span v-if="onSale" class="sale-save"
          >Tiết kiệm {{ money(product.price - product.discountedPrice) }}</span
        >
      </div>
      <div v-if="warranty" class="card-warranty"><ShopIcon name="shield" />Bảo hành {{ warranty }} tháng</div>
      <div class="product-bottom">
        <span :class="{ muted: !product.quantity }">{{
          product.quantity ? "● Sẵn hàng" : "Tạm hết hàng"
        }}</span
        ><span class="circle-arrow">↗</span>
      </div>
    </div>
  </RouterLink>
</template>
