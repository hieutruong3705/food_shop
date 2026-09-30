<script setup lang="ts">
import { ref } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const title = ref('Privacy Policy');
const content = ref(`
  <p class="mb-4">Welcome to Tech Shop. This privacy policy explains how we collect, use, and protect your personal information.</p>
  <h3 class="text-lg font-bold mt-6 mb-2">Information Collection</h3>
  <p class="mb-4">We collect information you provide directly to us when you create an account, make a purchase, or contact support.</p>
  <h3 class="text-lg font-bold mt-6 mb-2">Use of Information</h3>
  <p class="mb-4">We use the information we collect to provide, maintain, and improve our services, process transactions, and send related information.</p>
`);
</script>

<template>
  <div class="policy-page max-w-4xl mx-auto">
    <nav class="text-sm text-gray-500 mb-6">
      <router-link to="/" class="hover:text-blue-600">Home</router-link> &gt; <span>{{ title }}</span>
    </nav>
    
    <div class="bg-white p-8 rounded-lg border">
      <h1 class="text-3xl font-bold mb-8">{{ title }}</h1>
      <div class="prose max-w-none" v-html="content"></div>
    </div>
  </div>
</template>
