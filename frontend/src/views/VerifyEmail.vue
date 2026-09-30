<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const status = ref('verifying'); // verifying, success, error

onMounted(() => {
  setTimeout(() => {
    // mock verification
    status.value = 'success';
  }, 1000);
});
</script>

<template>
  <div class="max-w-md mx-auto mt-12 bg-white p-8 border rounded-lg shadow-sm text-center">
    <h1 class="text-2xl font-bold mb-6">Email Verification</h1>
    
    <div v-if="status === 'verifying'" class="text-gray-600">
      Verifying your email address, please wait...
    </div>
    
    <div v-else-if="status === 'success'" class="text-green-600">
      <p class="mb-4">Your email has been successfully verified!</p>
      <router-link to="/login" class="inline-block bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700">Go to Login</router-link>
    </div>
    
    <div v-else class="text-red-600">
      <p class="mb-4">Verification failed. The link may be invalid or expired.</p>
      <router-link to="/" class="inline-block bg-gray-600 text-white px-6 py-2 rounded hover:bg-gray-700">Back to Home</router-link>
    </div>
  </div>
</template>
