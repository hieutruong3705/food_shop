<script setup>
import { ref, watch } from 'vue';
import { imageUrl } from '../lib/shop';
const props = defineProps({ brand: Object });
const failed = ref(false);
watch(() => props.brand.logoUrl, () => failed.value = false);
</script>
<template><img v-if="brand.logoUrl && !failed" :src="imageUrl(brand.logoUrl)" :alt="brand.name" @error="failed = true" /><span v-else class="brand-wordmark">{{ brand.name }}</span></template>
