<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const route = useRoute();
const loading = ref(false);
const product = ref<any>(null);
const quantity = ref(1);
const activeTab = ref('description');

onMounted(() => {
  loading.value = true;
  setTimeout(() => {
    product.value = {
      id: route.params.id,
      name: 'Amazing Tech Gadget',
      price: 299.99,
      description: 'This is an amazing piece of technology.',
      images: ['https://via.placeholder.com/600', 'https://via.placeholder.com/150'],
    };
    loading.value = false;
  }, 500);
});
</script>

<template>
  <LoadingSpinner v-if="loading" />
  <div v-else-if="product" class="product-detail">
    <div class="flex flex-col md:flex-row gap-8 mb-12">
      <!-- Images -->
      <div class="w-full md:w-1/2">
        <img :src="product.images[0]" class="w-full h-auto rounded-lg mb-4 border" />
        <div class="flex gap-4">
          <img v-for="img in product.images" :key="img" :src="img" class="w-20 h-20 rounded border cursor-pointer hover:border-blue-500" />
        </div>
      </div>
      
      <!-- Info -->
      <div class="w-full md:w-1/2">
        <h1 class="text-3xl font-bold mb-4">{{ product.name }}</h1>
        <p class="text-2xl text-blue-600 font-bold mb-6">${{ product.price }}</p>
        <p class="text-gray-600 mb-6">{{ product.description }}</p>
        
        <div class="flex items-center gap-4 mb-6">
          <div class="flex items-center border rounded">
            <button class="px-4 py-2 hover:bg-gray-100" @click="quantity > 1 && quantity--">-</button>
            <input type="number" v-model="quantity" class="w-16 text-center border-x py-2" />
            <button class="px-4 py-2 hover:bg-gray-100" @click="quantity++">+</button>
          </div>
          <button class="bg-blue-600 text-white px-8 py-2 rounded hover:bg-blue-700 transition">Add to Cart</button>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="border-t pt-8">
      <div class="flex gap-8 mb-6 border-b">
        <button :class="['pb-2', activeTab === 'description' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500']" @click="activeTab = 'description'">Description</button>
        <button :class="['pb-2', activeTab === 'specs' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500']" @click="activeTab = 'specs'">Specifications</button>
      </div>
      <div>
        <p v-if="activeTab === 'description'">{{ product.description }}</p>
        <div v-else-if="activeTab === 'specs'">No specs available.</div>
      </div>
    </div>
  </div>
</template>
