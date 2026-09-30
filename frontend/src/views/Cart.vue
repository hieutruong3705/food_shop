<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const cartItems = ref([
  { id: 1, name: 'Product 1', price: 99.99, quantity: 1, image: 'https://via.placeholder.com/100' }
]);
const voucherCode = ref('');

const total = ref(99.99);
</script>

<template>
  <div class="cart-page max-w-4xl mx-auto">
    <h1 class="text-3xl font-bold mb-8">Shopping Cart</h1>
    
    <div v-if="cartItems.length > 0" class="flex flex-col md:flex-row gap-8">
      <div class="flex-1">
        <div v-for="item in cartItems" :key="item.id" class="flex items-center gap-4 border-b py-4">
          <img :src="item.image" class="w-20 h-20 rounded object-cover" />
          <div class="flex-1">
            <h3 class="font-semibold">{{ item.name }}</h3>
            <p class="text-gray-600">${{ item.price }}</p>
          </div>
          <div class="flex items-center border rounded">
            <button class="px-3 py-1 hover:bg-gray-100">-</button>
            <span class="px-3 py-1 border-x">{{ item.quantity }}</span>
            <button class="px-3 py-1 hover:bg-gray-100">+</button>
          </div>
          <button class="text-red-500 hover:text-red-700 ml-4">Remove</button>
        </div>
      </div>
      
      <div class="w-full md:w-80 shrink-0">
        <div class="bg-gray-50 p-6 rounded-lg border">
          <h3 class="text-xl font-bold mb-4">Summary</h3>
          <div class="flex justify-between mb-2">
            <span>Subtotal</span>
            <span>${{ total }}</span>
          </div>
          <div class="border-t mt-4 pt-4 mb-6">
            <div class="flex justify-between font-bold text-lg">
              <span>Total</span>
              <span>${{ total }}</span>
            </div>
          </div>
          <button @click="router.push('/checkout')" class="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 transition font-semibold">Proceed to Checkout</button>
        </div>
      </div>
    </div>
    
    <div v-else class="text-center py-12">
      <p class="text-xl text-gray-500 mb-6">Your cart is empty.</p>
      <button @click="router.push('/products')" class="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700">Continue Shopping</button>
    </div>
  </div>
</template>
