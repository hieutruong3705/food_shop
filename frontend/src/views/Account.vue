<script setup lang="ts">
import { ref } from 'vue';

const profile = ref({
  name: 'John Doe',
  email: 'john@example.com',
  phone: '1234567890',
  address: '123 Tech Street',
  avatar: 'https://via.placeholder.com/150'
});

const passwords = ref({
  current: '',
  new: '',
  confirm: ''
});

const saveProfile = () => {
  // save profile mock
};

const changePassword = () => {
  // change password mock
};
</script>

<template>
  <div class="account-page max-w-4xl mx-auto flex flex-col md:flex-row gap-8">
    
    <div class="w-full md:w-1/3">
      <div class="bg-white p-6 rounded-lg border text-center mb-6">
        <img :src="profile.avatar" class="w-32 h-32 rounded-full mx-auto mb-4 object-cover border" />
        <button class="text-sm bg-gray-100 px-4 py-2 rounded hover:bg-gray-200">Upload Avatar</button>
      </div>
    </div>
    
    <div class="flex-1 space-y-8">
      <div class="bg-white p-6 rounded-lg border">
        <h2 class="text-xl font-bold mb-6">Profile Information</h2>
        <form @submit.prevent="saveProfile" class="space-y-4">
          <div>
            <label class="block mb-1 text-sm font-medium">Full Name</label>
            <input v-model="profile.name" type="text" class="w-full border rounded px-4 py-2" required />
          </div>
          <div>
            <label class="block mb-1 text-sm font-medium">Email (readonly)</label>
            <input v-model="profile.email" type="email" class="w-full border rounded px-4 py-2 bg-gray-50 text-gray-500" readonly />
          </div>
          <div>
            <label class="block mb-1 text-sm font-medium">Phone</label>
            <input v-model="profile.phone" type="text" class="w-full border rounded px-4 py-2" />
          </div>
          <div>
            <label class="block mb-1 text-sm font-medium">Address</label>
            <textarea v-model="profile.address" class="w-full border rounded px-4 py-2" rows="3"></textarea>
          </div>
          <button type="submit" class="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700">Save Changes</button>
        </form>
      </div>
      
      <div class="bg-white p-6 rounded-lg border">
        <h2 class="text-xl font-bold mb-6">Change Password</h2>
        <form @submit.prevent="changePassword" class="space-y-4">
          <div>
            <label class="block mb-1 text-sm font-medium">Current Password</label>
            <input v-model="passwords.current" type="password" class="w-full border rounded px-4 py-2" required />
          </div>
          <div>
            <label class="block mb-1 text-sm font-medium">New Password</label>
            <input v-model="passwords.new" type="password" class="w-full border rounded px-4 py-2" required />
          </div>
          <div>
            <label class="block mb-1 text-sm font-medium">Confirm New Password</label>
            <input v-model="passwords.confirm" type="password" class="w-full border rounded px-4 py-2" required />
          </div>
          <button type="submit" class="bg-gray-800 text-white px-6 py-2 rounded hover:bg-gray-900">Update Password</button>
        </form>
      </div>
    </div>
    
  </div>
</template>
