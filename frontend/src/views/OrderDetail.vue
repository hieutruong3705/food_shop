<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const order = ref({
  id: route.params.id,
  date: '2023-10-28',
  status: 'PENDING',
  paymentMethod: 'COD',
  paymentStatus: 'UNPAID',
  items: [
    { id: 1, name: 'Product 1', price: 99.99, quantity: 2, subtotal: 199.98 }
  ],
  subtotal: 199.98,
  shipping: 10.00,
  total: 209.98
});

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
  <div class="order-detail max-w-4xl mx-auto">
    <div class="flex items-center justify-between mb-8">
      <h1 class="text-3xl font-bold">Order Details</h1>
      <button @click="router.push('/orders')" class="text-blue-600 hover:underline">&larr; Back to Orders</button>
    </div>
    
    <div class="bg-white p-6 rounded-lg border mb-6 flex flex-wrap gap-8 justify-between">
      <div>
        <p class="text-sm text-gray-500 mb-1">Order ID</p>
        <p class="font-semibold">{{ order.id }}</p>
      </div>
      <div>
        <p class="text-sm text-gray-500 mb-1">Date</p>
        <p class="font-semibold">{{ order.date }}</p>
      </div>
      <div>
        <p class="text-sm text-gray-500 mb-1">Status</p>
        <span :class="['px-2 py-1 rounded text-xs font-semibold', getStatusColor(order.status)]">
          {{ order.status }}
        </span>
      </div>
      <div>
        <p class="text-sm text-gray-500 mb-1">Payment</p>
        <p class="font-semibold">{{ order.paymentMethod }} - {{ order.paymentStatus }}</p>
      </div>
    </div>
    
    <div class="bg-white rounded-lg border overflow-hidden mb-6">
      <table class="w-full text-left">
        <thead class="bg-gray-50 border-b">
          <tr>
            <th class="px-6 py-3 font-medium">Product</th>
            <th class="px-6 py-3 font-medium">Price</th>
            <th class="px-6 py-3 font-medium">Qty</th>
            <th class="px-6 py-3 font-medium text-right">Subtotal</th>
          </tr>
        </thead>
        <tbody class="divide-y">
          <tr v-for="item in order.items" :key="item.id">
            <td class="px-6 py-4">{{ item.name }}</td>
            <td class="px-6 py-4">${{ item.price }}</td>
            <td class="px-6 py-4">{{ item.quantity }}</td>
            <td class="px-6 py-4 text-right">${{ item.subtotal }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="flex justify-end mb-6">
      <div class="w-full md:w-80 bg-gray-50 p-6 rounded-lg border">
        <div class="flex justify-between mb-2">
          <span>Subtotal</span>
          <span>${{ order.subtotal }}</span>
        </div>
        <div class="flex justify-between mb-2">
          <span>Shipping</span>
          <span>${{ order.shipping }}</span>
        </div>
        <div class="border-t mt-4 pt-4">
          <div class="flex justify-between font-bold text-lg">
            <span>Total</span>
            <span>${{ order.total }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="flex gap-4 justify-end">
      <button class="bg-gray-200 px-6 py-2 rounded hover:bg-gray-300">Download Invoice</button>
      <button v-if="order.status === 'PENDING'" class="bg-red-600 text-white px-6 py-2 rounded hover:bg-red-700">Cancel Order</button>
    </div>
  </div>
</template>
