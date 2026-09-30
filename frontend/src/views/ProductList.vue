<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import Pagination from '../components/Pagination.vue';

const router = useRouter();
const loading = ref(false);
const products = ref([]);
const searchQuery = ref('');
const sortBy = ref('newest');

onMounted(() => {
  loading.value = true;
  setTimeout(() => {
    products.value = [
      { id: 1, name: 'Product 1', price: 99.99, image: 'https://via.placeholder.com/300' },
      { id: 2, name: 'Product 2', price: 149.99, image: 'https://via.placeholder.com/300' },
      { id: 3, name: 'Product 3', price: 199.99, image: 'https://via.placeholder.com/300' },
      { id: 4, name: 'Product 4', price: 299.99, image: 'https://via.placeholder.com/300' },
    ] as any;
    loading.value = false;
  }, 500);
});
</script>

<template>
  <div class="product-list flex flex-col md:flex-row gap-8">
    <!-- Sidebar -->
    <aside class="w-full md:w-64 shrink-0">
      <div class="bg-white p-4 rounded-lg border">
        <h3 class="font-bold text-lg mb-4">Filters</h3>
        <div class="mb-4">
          <h4 class="font-semibold mb-2">Category</h4>
          <ul>
            <li><label><input type="checkbox" class="mr-2" /> Laptops</label></li>
            <li><label><input type="checkbox" class="mr-2" /> Phones</label></li>
          </ul>
        </div>
        <div>
          <h4 class="font-semibold mb-2">Price</h4>
          <input type="range" class="w-full" />
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1">
      <div class="flex flex-col sm:flex-row justify-between items-center mb-6 gap-4">
        <input v-model="searchQuery" type="text" placeholder="Search products..." class="border rounded px-4 py-2 w-full sm:w-auto flex-1" />
        <select v-model="sortBy" class="border rounded px-4 py-2 w-full sm:w-auto">
          <option value="newest">Newest</option>
          <option value="price_asc">Price: Low to High</option>
          <option value="price_desc">Price: High to Low</option>
        </select>
      </div>

      <LoadingSpinner v-if="loading" />

      <div v-else>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6 mb-8">
          <div v-for="product in products" :key="product.id" class="border rounded-lg p-4 hover:shadow-lg transition flex flex-col">
            <img :src="product.image" :alt="product.name" class="w-full h-48 object-cover mb-4 rounded cursor-pointer" @click="router.push(`/products/${product.id}`)" />
            <h3 class="font-semibold mb-2 cursor-pointer hover:text-blue-600" @click="router.push(`/products/${product.id}`)">{{ product.name }}</h3>
            <div class="mt-auto">
              <p class="text-blue-600 font-bold mb-4">${{ product.price }}</p>
              <button class="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">Add to Cart</button>
            </div>
          </div>
        </div>

        <Pagination :current-page="1" :total-pages="5" @page-change="() => {}" />
      </div>
    </main>
  </div>
</template>
