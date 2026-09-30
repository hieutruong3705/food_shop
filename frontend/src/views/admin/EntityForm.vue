<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminApi } from '@/api/admin';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import Toast from '@/components/Toast.vue';

const route = useRoute();
const router = useRouter();

const entity = computed(() => route.params.entity as string);
const idParam = computed(() => route.params.id as string);
const isEdit = computed(() => idParam.value && idParam.value !== 'new');

const loading = ref(false);
const saving = ref(false);
const toast = ref<{ message: string, type: 'success'|'error' } | null>(null);

const formData = ref<any>({});

const loadData = async () => {
  if (!isEdit.value) {
    formData.value = {}; // Reset for create
    return;
  }
  
  loading.value = true;
  try {
    const res = await adminApi.get(entity.value, idParam.value);
    formData.value = res.data || res;
  } catch (err) {
    console.error(err);
    toast.value = { message: 'Failed to load data', type: 'error' };
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadData();
});

const handleSubmit = async () => {
  saving.value = true;
  try {
    const payload = { ...formData.value };
    if (isEdit.value) payload.id = idParam.value;
    
    await adminApi.save(entity.value, payload);
    toast.value = { message: 'Saved successfully', type: 'success' };
    setTimeout(() => {
      router.push(`/admin/${entity.value}`);
    }, 1000);
  } catch (err) {
    console.error(err);
    toast.value = { message: 'Failed to save', type: 'error' };
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div class="max-w-3xl mx-auto">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold capitalize">{{ isEdit ? 'Edit' : 'Create' }} {{ entity }}</h2>
      <button @click="router.push(`/admin/${entity}`)" class="text-gray-600 hover:text-gray-900 font-medium text-sm">
        &larr; Back to List
      </button>
    </div>

    <div v-if="loading" class="flex justify-center p-12">
      <LoadingSpinner />
    </div>
    
    <div v-else class="bg-white rounded-lg shadow p-6 border border-gray-200">
      <form @submit.prevent="handleSubmit" class="space-y-6">
        
        <!-- Common basic fields based on entity -->
        <template v-if="entity === 'products'">
          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700">Name</label>
              <input v-model="formData.name" type="text" required class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700">Price</label>
              <input v-model.number="formData.price" type="number" required class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700">Category ID</label>
              <input v-model.number="formData.categoryId" type="number" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
            </div>
            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700">Description</label>
              <textarea v-model="formData.description" rows="4" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"></textarea>
            </div>
          </div>
        </template>
        
        <template v-else-if="entity === 'categories' || entity === 'brands'">
          <div class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-gray-700">Name</label>
              <input v-model="formData.name" type="text" required class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700">Slug</label>
              <input v-model="formData.slug" type="text" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700">Description</label>
              <textarea v-model="formData.description" rows="3" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700">Image URL</label>
              <input v-model="formData.imageUrl" type="text" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
            </div>
          </div>
        </template>
        
        <template v-else-if="entity === 'users'">
          <div class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-gray-700">Email</label>
              <input v-model="formData.email" type="email" required :disabled="isEdit" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700">First Name</label>
                <input v-model="formData.firstName" type="text" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700">Last Name</label>
                <input v-model="formData.lastName" type="text" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700">Role</label>
              <select v-model="formData.role" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm">
                <option value="USER">User</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
          </div>
        </template>

        <!-- Generic fallback for other entities -->
        <template v-else>
          <div class="space-y-4">
            <div v-for="key in Object.keys(formData).filter(k => k !== 'id')" :key="key">
              <label class="block text-sm font-medium text-gray-700 capitalize">{{ key }}</label>
              <input v-model="formData[key]" type="text" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" />
            </div>
            <div v-if="!isEdit" class="text-sm text-gray-500 italic">
              Please enter the raw JSON representation for this entity.
              <textarea v-model="formData.raw" rows="6" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 font-mono text-sm"></textarea>
            </div>
          </div>
        </template>

        <div class="pt-5 border-t border-gray-200 flex justify-end space-x-3">
          <button type="button" @click="router.push(`/admin/${entity}`)" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50">
            Cancel
          </button>
          <button type="submit" :disabled="saving" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50">
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </form>
    </div>
    
    <Toast 
      v-if="toast" 
      :message="toast.message" 
      :type="toast.type" 
      @close="toast = null" 
      class="fixed bottom-4 right-4 z-50"
    />
  </div>
</template>
