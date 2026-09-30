<script setup>
import ProductCard from '../ProductCard.vue';
import ShopIcon from '../ShopIcon.vue';

// The shop's best sellers, ranked by paid or cash-on-delivery units in confirmed orders.
defineProps({ rows: { type: Array, required: true } });
</script>

<template>
  <section v-if="rows.length" class="home-block" aria-labelledby="best-title">
    <div class="retail-section-title"><h2 id="best-title">Bán chạy nhất</h2><RouterLink to="/products?sort=popular">Xem tất cả <ShopIcon name="arrow" /></RouterLink></div>
    <div class="product-grid"><ProductCard v-for="row in rows" :key="row.product.id" :product="row.product" /></div>
  </section>
</template>
