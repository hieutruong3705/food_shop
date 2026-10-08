<script setup>
import { onMounted, onUnmounted, ref } from 'vue';

// A floating button that brings the reader back to the top of the page; it shows once the page is scrolled.
// `lifted` leaves room for the chat button, which sits in the same corner of the storefront.
defineProps({ lifted: { type: Boolean, default: false } });
const shown = ref(false);
const update = () => { shown.value = window.scrollY > 400; };
const toTop = () => window.scrollTo({ top: 0, behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
onMounted(() => { update(); window.addEventListener('scroll', update, { passive: true }); });
onUnmounted(() => window.removeEventListener('scroll', update));
</script>

<template>
  <Transition name="back-to-top">
    <button v-if="shown" type="button" class="back-to-top" :class="{ lifted }" aria-label="Về đầu trang" title="Về đầu trang" @click="toTop"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 19V5M5 12l7-7 7 7" /></svg></button>
  </Transition>
</template>
