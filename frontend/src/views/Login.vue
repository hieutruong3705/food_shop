<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const email = ref('');
const password = ref('');
const error = ref('');

const login = () => {
  if (email.value && password.value) {
    router.push('/');
  } else {
    error.value = 'Invalid credentials';
  }
};
</script>

<template>
  <div class="login-page max-w-md mx-auto mt-12 bg-white p-8 border rounded-lg shadow-sm">
    <h1 class="text-2xl font-bold mb-6 text-center">Login</h1>
    
    <div v-if="error" class="bg-red-100 text-red-600 p-3 rounded mb-4 text-sm">{{ error }}</div>
    
    <form @submit.prevent="login" class="space-y-4">
      <div>
        <label class="block mb-1 text-sm font-medium">Email</label>
        <input v-model="email" type="email" class="w-full border rounded px-4 py-2" required />
      </div>
      <div>
        <label class="block mb-1 text-sm font-medium">Password</label>
        <input v-model="password" type="password" class="w-full border rounded px-4 py-2" required />
      </div>
      
      <div class="flex justify-between items-center text-sm">
        <label class="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" /> Remember me
        </label>
        <router-link to="/forgot-password" class="text-blue-600 hover:underline">Forgot password?</router-link>
      </div>
      
      <button type="submit" class="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">Login</button>
    </form>
    
    <p class="mt-6 text-center text-sm text-gray-600">
      Don't have an account? <router-link to="/register" class="text-blue-600 hover:underline">Register</router-link>
    </p>
  </div>
</template>
