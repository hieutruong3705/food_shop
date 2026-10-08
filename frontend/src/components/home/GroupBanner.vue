<script setup>
import { imageUrl, imageError } from '../../lib/shop';

// An advertising banner that takes the first one or two product cells of a home product group.
// Where the cells do not have the banner's shape, the banner is shown whole on a panel of its own corner
// colour: the second image is that panel, the banner's top-left corner enlarged.
defineProps({ banner: { type: Object, required: true }, cells: { type: Number, default: 2 } });
</script>

<template>
  <a class="group-banner" :class="cells === 1 ? 'single' : 'double'" :href="banner.link || '/products'">
    <img class="group-banner-fill" :src="imageUrl(banner.imageUrl)" alt="" aria-hidden="true" loading="lazy" />
    <img class="group-banner-image" :src="imageUrl(banner.imageUrl)" :alt="banner.name" loading="lazy" @error="imageError" />
  </a>
</template>
