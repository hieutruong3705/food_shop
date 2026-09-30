<script setup>
import { onMounted, ref } from 'vue';
import ProductCard from '../ProductCard.vue';
import { recentProducts } from '../../lib/recent';

// Products this shopper opened lately; read from this browser, so it needs no request.
const items = ref([]);
onMounted(() => { items.value = recentProducts().slice(0, 4); });
</script>

<template>
  <section v-if="items.length" class="home-block" aria-labelledby="recent-title">
    <div class="retail-section-title"><h2 id="recent-title">Bạn vừa xem</h2></div>
    <div class="product-grid"><ProductCard v-for="p in items" :key="p.id" :product="p" /></div>
  </section>
</template>
