<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { adminApi } from '@/api/admin';

const stats = ref({
  totalUsers: 0,
  totalProducts: 0,
  totalOrders: 0,
  totalRevenue: 0,
});

const recentOrders = ref<any[]>([]);
const topProducts = ref<any[]>([]);
const loading = ref(false);

const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

onMounted(async () => {
  loading.value = true;
  try {
    // These could be real API calls to a dashboard endpoint. For now, fetching basics.
    const [users, products, orders] = await Promise.all([
      adminApi.list('users', { page: 0, size: 1 }),
      adminApi.list('products', { page: 0, size: 5 }),
      adminApi.list('orders', { page: 0, size: 10 }),
    ]);
    
    stats.value.totalUsers = users.data.totalElements || 0;
    stats.value.totalProducts = products.data.totalElements || 0;
    stats.value.totalOrders = orders.data.totalElements || 0;
    
    recentOrders.value = orders.data.content || [];
    topProducts.value = products.data.content || [];
  } catch (error) {
    console.error('Failed to load dashboard data', error);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div v-if="loading" class="flex justify-center py-10">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
  </div>
  <div v-else class="space-y-6">
    <h2 class="text-2xl font-bold text-gray-900">Dashboard Overview</h2>
    
    <!-- Stats Row -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-white p-6 rounded-lg shadow border border-gray-100">
        <h3 class="text-gray-500 text-sm font-medium">Total Users</h3>
        <p class="text-3xl font-bold text-gray-900 mt-2">{{ stats.totalUsers }}</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow border border-gray-100">
        <h3 class="text-gray-500 text-sm font-medium">Total Products</h3>
        <p class="text-3xl font-bold text-gray-900 mt-2">{{ stats.totalProducts }}</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow border border-gray-100">
        <h3 class="text-gray-500 text-sm font-medium">Total Orders</h3>
        <p class="text-3xl font-bold text-gray-900 mt-2">{{ stats.totalOrders }}</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow border border-gray-100">
        <h3 class="text-gray-500 text-sm font-medium">Total Revenue</h3>
        <p class="text-3xl font-bold text-green-600 mt-2">{{ formatCurrency(stats.totalRevenue) }}</p>
      </div>
    </div>
    
    <!-- Revenue Chart -->
    <div class="bg-white p-6 rounded-lg shadow border border-gray-100">
      <h3 class="text-lg font-bold text-gray-900 mb-4">Revenue Chart</h3>
      <div class="h-64 flex items-end justify-between space-x-2">
        <div v-for="i in 12" :key="i" class="w-full bg-blue-100 hover:bg-blue-200 rounded-t flex flex-col justify-end" :style="{ height: `${Math.random() * 80 + 20}%` }">
          <div class="text-xs text-center text-gray-500 mb-1 hidden sm:block">M{{i}}</div>
        </div>
      </div>
    </div>

    <!-- Lists -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="bg-white rounded-lg shadow border border-gray-100">
        <div class="px-4 py-5 sm:px-6 border-b border-gray-200">
          <h3 class="text-lg leading-6 font-medium text-gray-900">Recent Orders</h3>
        </div>
        <ul class="divide-y divide-gray-200">
          <li v-for="order in recentOrders" :key="order.id" class="px-4 py-4 sm:px-6 hover:bg-gray-50">
            <div class="flex items-center justify-between">
              <p class="text-sm font-medium text-blue-600 truncate">Order #{{ order.id }}</p>
              <div class="ml-2 flex-shrink-0 flex">
                <p class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                  {{ order.status }}
                </p>
              </div>
            </div>
            <div class="mt-2 sm:flex sm:justify-between">
              <div class="sm:flex">
                <p class="flex items-center text-sm text-gray-500">
                  Total: {{ formatCurrency(order.totalAmount || 0) }}
                </p>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-100">
        <div class="px-4 py-5 sm:px-6 border-b border-gray-200">
          <h3 class="text-lg leading-6 font-medium text-gray-900">Top Products</h3>
        </div>
        <ul class="divide-y divide-gray-200">
          <li v-for="product in topProducts" :key="product.id" class="px-4 py-4 sm:px-6 hover:bg-gray-50">
            <div class="flex items-center">
              <div class="flex-shrink-0 h-10 w-10">
                <img class="h-10 w-10 rounded object-cover" :src="product.images?.[0] || 'https://via.placeholder.com/40'" alt="" />
              </div>
              <div class="ml-4 flex-1">
                <div class="flex items-center justify-between">
                  <p class="text-sm font-medium text-gray-900">{{ product.name }}</p>
                  <p class="text-sm text-gray-500">{{ formatCurrency(product.price) }}</p>
                </div>
              </div>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
