<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const router = useRouter();
const loading = ref(false);
const featuredProducts = ref([]);
const newArrivals = ref([]);
const categories = ref([
  { id: 1, name: 'Laptops', image: 'https://via.placeholder.com/150' },
  { id: 2, name: 'Phones', image: 'https://via.placeholder.com/150' },
  { id: 3, name: 'Accessories', image: 'https://via.placeholder.com/150' },
]);

onMounted(async () => {
  loading.value = true;
  // Mock data for now, would fetch from API
  setTimeout(() => {
    featuredProducts.value = [
      { id: 1, name: 'Product 1', price: 99.99, image: 'https://via.placeholder.com/300' },
      { id: 2, name: 'Product 2', price: 149.99, image: 'https://via.placeholder.com/300' },
    ] as any;
    newArrivals.value = [
      { id: 3, name: 'Product 3', price: 199.99, image: 'https://via.placeholder.com/300' },
      { id: 4, name: 'Product 4', price: 299.99, image: 'https://via.placeholder.com/300' },
    ] as any;
    loading.value = false;
  }, 500);
});
</script>

<template>
  <div class="home">
    <!-- Hero Banner -->
    <div class="bg-gray-900 text-white p-12 text-center rounded-lg mb-8">
      <h1 class="text-4xl font-bold mb-4">Welcome to Tech Shop</h1>
      <p class="text-xl">Discover the latest in technology.</p>
    </div>

    <!-- Featured Categories -->
    <section class="mb-12">
      <h2 class="text-2xl font-bold mb-6">Categories</h2>
      <div class="grid grid-cols-2 md:grid-cols-3 gap-6">
        <div v-for="cat in categories" :key="cat.id" 
             @click="router.push(`/products?category=${cat.id}`)"
             class="cursor-pointer border rounded-lg p-4 text-center hover:shadow-lg transition">
          <img :src="cat.image" :alt="cat.name" class="mx-auto mb-4 w-24 h-24 object-cover rounded-full" />
          <h3 class="font-semibold">{{ cat.name }}</h3>
        </div>
      </div>
    </section>

    <LoadingSpinner v-if="loading" />

    <div v-else>
      <!-- Featured Products -->
      <section class="mb-12">
        <h2 class="text-2xl font-bold mb-6">Featured Products</h2>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div v-for="product in featuredProducts" :key="product.id" class="border rounded-lg p-4 hover:shadow-lg transition">
            <img :src="product.image" :alt="product.name" class="w-full h-48 object-cover mb-4 rounded" />
            <h3 class="font-semibold mb-2">{{ product.name }}</h3>
            <p class="text-blue-600 font-bold mb-4">${{ product.price }}</p>
            <button @click="router.push(`/products/${product.id}`)" class="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">View Details</button>
          </div>
        </div>
      </section>

      <!-- New Arrivals -->
      <section class="mb-12">
        <h2 class="text-2xl font-bold mb-6">New Arrivals</h2>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div v-for="product in newArrivals" :key="product.id" class="border rounded-lg p-4 hover:shadow-lg transition">
            <img :src="product.image" :alt="product.name" class="w-full h-48 object-cover mb-4 rounded" />
            <h3 class="font-semibold mb-2">{{ product.name }}</h3>
            <p class="text-blue-600 font-bold mb-4">${{ product.price }}</p>
            <button @click="router.push(`/products/${product.id}`)" class="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">View Details</button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
