<script setup>
import BrandMark from '../BrandMark.vue';

defineProps({ brands: { type: Array, required: true } });
</script>

<template>
  <section v-if="brands.length" class="home-block" aria-labelledby="brands-title">
    <div class="retail-section-title"><h2 id="brands-title">Thương hiệu nổi bật</h2></div>
    <div class="brand-strip">
      <RouterLink v-for="b in brands" :key="b.id" :to="{ path: '/products', query: { brandIds: b.id } }" :aria-label="`${b.name}, ${b.count} sản phẩm`">
        <span class="brand-logo"><BrandMark :brand="b" /></span>
        <small>{{ b.count }} sản phẩm</small>
      </RouterLink>
    </div>
  </section>
</template>
