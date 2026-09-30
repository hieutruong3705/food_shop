<script setup>
import ProductCard from '../ProductCard.vue';
import ShopIcon from '../ShopIcon.vue';

// One row of four products for each of the busiest categories.
defineProps({ rows: { type: Array, required: true } });
</script>

<template>
  <template v-if="rows.length">
    <section v-for="row in rows" :key="row.category.id" class="home-block" :aria-label="row.category.name">
      <div class="retail-section-title">
        <h2>{{ row.category.name }} <small class="row-count">{{ row.category.productCount }} sản phẩm</small></h2>
        <RouterLink :to="{ path: '/products', query: { categoryId: row.category.id } }">Xem tất cả {{ row.category.name.toLowerCase() }} <ShopIcon name="arrow" /></RouterLink>
      </div>
      <div class="product-grid"><ProductCard v-for="p in row.products" :key="p.id" :product="p" /></div>
    </section>
  </template>
</template>
