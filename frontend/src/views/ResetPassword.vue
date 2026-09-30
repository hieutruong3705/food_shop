<script setup lang="ts">
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();
const password = ref('');
const confirmPassword = ref('');
const error = ref('');

const submit = () => {
  if (password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match';
    return;
  }
  // mock success
  router.push('/login');
};
</script>

<template>
  <div class="max-w-md mx-auto mt-12 bg-white p-8 border rounded-lg shadow-sm">
    <h1 class="text-2xl font-bold mb-6 text-center">Reset Password</h1>
    
    <div v-if="error" class="bg-red-100 text-red-600 p-3 rounded mb-4 text-sm">{{ error }}</div>
    
    <form @submit.prevent="submit" class="space-y-4">
      <div>
        <label class="block mb-1 text-sm font-medium">New Password</label>
        <input v-model="password" type="password" class="w-full border rounded px-4 py-2" required />
      </div>
      <div>
        <label class="block mb-1 text-sm font-medium">Confirm New Password</label>
        <input v-model="confirmPassword" type="password" class="w-full border rounded px-4 py-2" required />
      </div>
      <button type="submit" class="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">Reset Password</button>
    </form>
  </div>
</template>
