<script setup lang="ts">
import { ref } from 'vue';

const form = ref({
  name: '',
  email: '',
  password: '',
  confirmPassword: ''
});
const success = ref(false);

const register = () => {
  if (form.value.password === form.value.confirmPassword) {
    success.value = true;
  }
};
</script>

<template>
  <div class="register-page max-w-md mx-auto mt-12 bg-white p-8 border rounded-lg shadow-sm">
    <h1 class="text-2xl font-bold mb-6 text-center">Register</h1>
    
    <div v-if="success" class="bg-green-100 text-green-700 p-4 rounded text-center">
      Registration successful! Please check your email for verification.
      <br><br>
      <router-link to="/login" class="text-green-800 underline font-semibold">Go to Login</router-link>
    </div>
    
    <form v-else @submit.prevent="register" class="space-y-4">
      <div>
        <label class="block mb-1 text-sm font-medium">Full Name</label>
        <input v-model="form.name" type="text" class="w-full border rounded px-4 py-2" required />
      </div>
      <div>
        <label class="block mb-1 text-sm font-medium">Email</label>
        <input v-model="form.email" type="email" class="w-full border rounded px-4 py-2" required />
      </div>
      <div>
        <label class="block mb-1 text-sm font-medium">Password</label>
        <input v-model="form.password" type="password" class="w-full border rounded px-4 py-2" required />
      </div>
      <div>
        <label class="block mb-1 text-sm font-medium">Confirm Password</label>
        <input v-model="form.confirmPassword" type="password" class="w-full border rounded px-4 py-2" required />
      </div>
      
      <button type="submit" class="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">Register</button>
    </form>
    
    <p v-if="!success" class="mt-6 text-center text-sm text-gray-600">
      Already have an account? <router-link to="/login" class="text-blue-600 hover:underline">Login</router-link>
    </p>
  </div>
</template>
