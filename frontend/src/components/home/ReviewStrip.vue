<script setup>
import { date } from '../../lib/shop';

// Recent good reviews. Reviews can only be written after an order is completed.
defineProps({ reviews: { type: Array, required: true } });
const day = (value) => date(value).split(',')[0];
</script>

<template>
  <section v-if="reviews.length" class="home-block" aria-labelledby="reviews-title">
    <div class="retail-section-title"><h2 id="reviews-title">Khách hàng nói gì</h2></div>
    <div class="review-strip">
      <figure v-for="(r, i) in reviews" :key="i">
        <div class="stars" :aria-label="`${r.rating} trên 5 sao`"><span aria-hidden="true">{{ '★'.repeat(r.rating) }}<i>{{ '★'.repeat(5 - r.rating) }}</i></span></div>
        <blockquote>{{ r.content }}</blockquote>
        <figcaption><strong>{{ r.author }}</strong><RouterLink :to="`/products/${r.productId}`">{{ r.productName }}</RouterLink><small>{{ day(r.createdAt) }}</small></figcaption>
      </figure>
    </div>
  </section>
</template>
