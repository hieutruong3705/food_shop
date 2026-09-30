<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import Pagination from '../components/Pagination.vue';

const router = useRouter();
const orders = ref([
  { id: 'ORD-123', date: '2023-10-27', total: 199.98, status: 'DELIVERED', paymentStatus: 'PAID' },
  { id: 'ORD-124', date: '2023-10-28', total: 299.99, status: 'PENDING', paymentStatus: 'UNPAID' }
]);

const getStatusColor = (status: string) => {
  switch (status) {
    case 'PENDING': return 'bg-yellow-100 text-yellow-800';
    case 'DELIVERED': return 'bg-green-100 text-green-800';
    case 'CANCELLED': return 'bg-red-100 text-red-800';
    default: return 'bg-gray-100 text-gray-800';
  }
};
</script>

<template>
  <div class="orders-page max-w-4xl mx-auto">
    <h1 class="text-3xl font-bold mb-8">My Orders</h1>
    
    <div class="bg-white rounded-lg border overflow-hidden mb-6">
      <table class="w-full text-left">
        <thead class="bg-gray-50 border-b">
          <tr>
            <th class="px-6 py-3 font-medium">Order ID</th>
            <th class="px-6 py-3 font-medium">Date</th>
            <th class="px-6 py-3 font-medium">Total</th>
            <th class="px-6 py-3 font-medium">Status</th>
            <th class="px-6 py-3 font-medium">Action</th>
          </tr>
        </thead>
        <tbody class="divide-y">
          <tr v-for="order in orders" :key="order.id" class="hover:bg-gray-50">
            <td class="px-6 py-4">{{ order.id }}</td>
            <td class="px-6 py-4">{{ order.date }}</td>
            <td class="px-6 py-4">${{ order.total }}</td>
            <td class="px-6 py-4">
              <span :class="['px-2 py-1 rounded text-xs font-semibold', getStatusColor(order.status)]">
                {{ order.status }}
              </span>
            </td>
            <td class="px-6 py-4">
              <button @click="router.push(`/orders/${order.id}`)" class="text-blue-600 hover:underline">View</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <Pagination :current-page="1" :total-pages="3" @page-change="() => {}" />
  </div>
</template>
