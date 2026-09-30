<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { 
  HomeIcon, 
  ShoppingBagIcon, 
  TagIcon, 
  StarIcon,
  ShoppingCartIcon,
  UsersIcon,
  TicketIcon,
  MegaphoneIcon,
  PhotoIcon,
  DocumentTextIcon,
  ChatBubbleLeftEllipsisIcon
} from '@heroicons/vue/24/outline';

const authStore = useAuthStore();

const menuItems = [
  { name: 'Dashboard', path: '/admin', icon: HomeIcon },
  { name: 'Products', path: '/admin/products', icon: ShoppingBagIcon },
  { name: 'Categories', path: '/admin/categories', icon: TagIcon },
  { name: 'Brands', path: '/admin/brands', icon: StarIcon },
  { name: 'Orders', path: '/admin/orders', icon: ShoppingCartIcon },
  { name: 'Users', path: '/admin/users', icon: UsersIcon },
  { name: 'Vouchers', path: '/admin/vouchers', icon: TicketIcon },
  { name: 'Promotions', path: '/admin/promotions', icon: MegaphoneIcon },
  { name: 'Banners', path: '/admin/banners', icon: PhotoIcon },
  { name: 'Policies', path: '/admin/policies', icon: DocumentTextIcon },
  { name: 'Reviews', path: '/admin/reviews', icon: ChatBubbleLeftEllipsisIcon },
];
</script>

<template>
  <div class="min-h-screen flex bg-gray-100">
    <!-- Sidebar -->
    <aside class="w-64 bg-gray-900 text-white flex flex-col hidden md:flex">
      <div class="h-16 flex items-center justify-center border-b border-gray-800">
        <span class="text-xl font-bold">TechShop Admin</span>
      </div>
      <div class="flex-1 overflow-y-auto py-4">
        <nav class="px-2 space-y-1">
          <RouterLink 
            v-for="item in menuItems" 
            :key="item.path"
            :to="item.path"
            class="group flex items-center px-2 py-2 text-sm font-medium rounded-md"
            :class="$route.path === item.path ? 'bg-gray-800 text-white' : 'text-gray-300 hover:bg-gray-700 hover:text-white'"
          >
            <component :is="item.icon" class="mr-3 h-6 w-6 flex-shrink-0" :class="$route.path === item.path ? 'text-white' : 'text-gray-400 group-hover:text-gray-300'" />
            {{ item.name }}
          </RouterLink>
        </nav>
      </div>
      <div class="p-4 border-t border-gray-800">
        <RouterLink to="/" class="flex items-center text-sm font-medium text-gray-300 hover:text-white">
          Return to Store
        </RouterLink>
      </div>
    </aside>

    <!-- Main Wrapper -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- Top navbar -->
      <header class="h-16 bg-white shadow-sm flex items-center justify-between px-4 sm:px-6 lg:px-8">
        <div class="flex items-center">
          <!-- Mobile menu button can go here -->
          <h1 class="text-xl font-semibold text-gray-900 capitalize">
            {{ $route.name?.toString().replace('admin-', '') || 'Admin' }}
          </h1>
        </div>
        <div class="flex items-center space-x-4">
          <span class="text-sm font-medium text-gray-700">{{ authStore.user?.firstName }} {{ authStore.user?.lastName }}</span>
          <button @click="authStore.logout()" class="text-sm font-medium text-gray-500 hover:text-gray-700">Logout</button>
        </div>
      </header>

      <!-- Main content -->
      <main class="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
        <div class="bg-white shadow rounded-lg p-6 min-h-full">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>
