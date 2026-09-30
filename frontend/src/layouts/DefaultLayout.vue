<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { onMounted } from 'vue';
import { ShoppingCartIcon, UserIcon } from '@heroicons/vue/24/outline';

const authStore = useAuthStore();
const cartStore = useCartStore();

onMounted(() => {
  if (authStore.isLoggedIn) {
    cartStore.fetchCart();
  }
});
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- Header -->
    <header class="bg-white shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center">
            <RouterLink to="/" class="flex-shrink-0 flex items-center">
              <span class="text-2xl font-bold text-primary">TechShop</span>
            </RouterLink>
            <nav class="hidden md:ml-6 md:flex md:space-x-8">
              <RouterLink to="/" class="text-gray-900 hover:text-primary px-3 py-2 rounded-md text-sm font-medium">Home</RouterLink>
              <RouterLink to="/products" class="text-gray-900 hover:text-primary px-3 py-2 rounded-md text-sm font-medium">Products</RouterLink>
            </nav>
          </div>
          
          <div class="flex items-center space-x-4">
            <div class="relative">
              <input type="text" placeholder="Search..." class="w-full sm:w-64 px-4 py-2 border rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-primary">
            </div>
            
            <RouterLink to="/cart" class="relative text-gray-700 hover:text-primary">
              <ShoppingCartIcon class="h-6 w-6" />
              <span v-if="cartStore.itemCount > 0" class="absolute -top-2 -right-2 bg-danger text-white text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center">
                {{ cartStore.itemCount }}
              </span>
            </RouterLink>

            <template v-if="authStore.isLoggedIn">
              <div class="relative group">
                <button class="flex items-center space-x-1 text-gray-700 hover:text-primary">
                  <UserIcon class="h-6 w-6" />
                </button>
                <div class="absolute right-0 w-48 mt-2 py-2 bg-white rounded-md shadow-xl hidden group-hover:block z-50">
                  <div class="px-4 py-2 border-b text-sm text-gray-700 font-medium">{{ authStore.user?.firstName }} {{ authStore.user?.lastName }}</div>
                  <RouterLink to="/account" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">Account</RouterLink>
                  <RouterLink to="/orders" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">Orders</RouterLink>
                  <RouterLink v-if="authStore.isAdmin" to="/admin" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 text-primary font-medium">Admin Dashboard</RouterLink>
                  <button @click="authStore.logout()" class="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">Logout</button>
                </div>
              </div>
            </template>
            <template v-else>
              <RouterLink to="/login" class="text-gray-900 hover:text-primary text-sm font-medium">Login</RouterLink>
              <RouterLink to="/register" class="bg-primary hover:bg-primary-dark text-white px-4 py-2 rounded-md text-sm font-medium transition-colors">Register</RouterLink>
            </template>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-grow w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <RouterView />
    </main>

    <!-- Footer -->
    <footer class="bg-white border-t mt-auto">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div class="md:flex md:items-center md:justify-between">
          <div class="flex justify-center md:justify-start space-x-6 md:order-2">
            <span class="text-gray-400">&copy; 2024 TechShop. All rights reserved.</span>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>
