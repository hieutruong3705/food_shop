<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const form = ref({
  name: '',
  phone: '',
  address: '',
  paymentMethod: 'COD'
});

const placeOrder = () => {
  // simulate order
  router.push('/orders');
};
</script>

<template>
  <div class="checkout max-w-4xl mx-auto">
    <h1 class="text-3xl font-bold mb-8">Checkout</h1>
    
    <div class="flex flex-col md:flex-row gap-8">
      <div class="flex-1">
        <div class="bg-white p-6 rounded-lg border mb-6">
          <h2 class="text-xl font-bold mb-4">Shipping Information</h2>
          <form class="space-y-4">
            <div>
              <label class="block mb-1 font-medium">Receiver Name</label>
              <input v-model="form.name" type="text" class="w-full border rounded px-4 py-2" required />
            </div>
            <div>
              <label class="block mb-1 font-medium">Phone</label>
              <input v-model="form.phone" type="text" class="w-full border rounded px-4 py-2" required />
            </div>
            <div>
              <label class="block mb-1 font-medium">Address</label>
              <textarea v-model="form.address" class="w-full border rounded px-4 py-2" rows="3" required></textarea>
            </div>
          </form>
        </div>
        
        <div class="bg-white p-6 rounded-lg border">
          <h2 class="text-xl font-bold mb-4">Payment Method</h2>
          <div class="space-y-2">
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="radio" v-model="form.paymentMethod" value="COD" />
              Cash on Delivery (COD)
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="radio" v-model="form.paymentMethod" value="VNPAY" />
              VNPay
            </label>
          </div>
        </div>
      </div>
      
      <div class="w-full md:w-80 shrink-0">
        <div class="bg-gray-50 p-6 rounded-lg border">
          <h3 class="text-xl font-bold mb-4">Order Summary</h3>
          <div class="flex justify-between mb-2">
            <span>Subtotal</span>
            <span>$99.99</span>
          </div>
          <div class="border-t mt-4 pt-4 mb-6">
            <div class="flex justify-between font-bold text-lg">
              <span>Total</span>
              <span>$99.99</span>
            </div>
          </div>
          <button @click="placeOrder" class="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 transition font-semibold">Place Order</button>
        </div>
      </div>
    </div>
  </div>
</template>
