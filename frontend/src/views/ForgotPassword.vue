<script setup lang="ts">
import { ref } from 'vue';

const email = ref('');
const success = ref(false);

const submit = () => {
  success.value = true;
};
</script>

<template>
  <div class="max-w-md mx-auto mt-12 bg-white p-8 border rounded-lg shadow-sm">
    <h1 class="text-2xl font-bold mb-6 text-center">Forgot Password</h1>
    
    <div v-if="success" class="bg-green-100 text-green-700 p-4 rounded text-center">
      Please check your email for reset instructions.
    </div>
    
    <form v-else @submit.prevent="submit" class="space-y-4">
      <p class="text-sm text-gray-600 mb-4">Enter your email address and we will send you a link to reset your password.</p>
      <div>
        <label class="block mb-1 text-sm font-medium">Email</label>
        <input v-model="email" type="email" class="w-full border rounded px-4 py-2" required />
      </div>
      <button type="submit" class="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">Send Reset Link</button>
    </form>
    
    <div class="mt-6 text-center">
      <router-link to="/login" class="text-sm text-blue-600 hover:underline">Back to Login</router-link>
    </div>
  </div>
</template>
