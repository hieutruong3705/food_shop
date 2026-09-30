<script setup>
import { computed } from "vue";
import { money, imageUrl, imageError, saleRate, rateText } from "../lib/shop";
const props = defineProps({ product: Object });
const onSale = computed(() => props.product.discountedPrice < props.product.price);
const rate = computed(() => rateText(saleRate(props.product)));
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
      <strong>{{ money(product.discountedPrice) }}</strong
      ><del v-if="onSale">{{
        money(product.price)
      }}</del
      ><span v-if="onSale" class="sale-save"
        >Tiết kiệm {{ money(product.price - product.discountedPrice) }}</span
      >
      <div class="product-bottom">
        <span :class="{ muted: !product.quantity }">{{
          product.quantity ? "● Sẵn hàng" : "Tạm hết hàng"
        }}</span
        ><span class="circle-arrow">↗</span>
      </div>
    </div>
  </RouterLink>
</template>
