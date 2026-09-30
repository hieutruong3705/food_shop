<script setup>
import { money, imageUrl, imageError, discountPercent } from "../lib/shop";
defineProps({ product: Object });
</script>
<template>
  <RouterLink :to="`/products/${product.id}`" class="product-card">
    <div class="product-image">
      <span v-if="product.discountedPrice < product.price" class="badge"
        >Giảm {{ discountPercent(product.price, product.discountedPrice) }}%</span
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
      ><del v-if="product.discountedPrice < product.price">{{
        money(product.price)
      }}</del>
      <div class="product-bottom">
        <span :class="{ muted: !product.quantity }">{{
          product.quantity ? "● Sẵn hàng" : "Tạm hết hàng"
        }}</span
        ><span class="circle-arrow">↗</span>
      </div>
    </div>
  </RouterLink>
</template>
