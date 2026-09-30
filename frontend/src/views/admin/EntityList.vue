<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminApi } from '@/api/admin';
import Pagination from '@/components/Pagination.vue';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import Toast from '@/components/Toast.vue';
import { PencilIcon, TrashIcon, EyeIcon } from '@heroicons/vue/24/outline';

const route = useRoute();
const router = useRouter();

const entity = computed(() => route.params.entity as string);

const data = ref<any[]>([]);
const totalElements = ref(0);
const totalPages = ref(0);
const currentPage = ref(0);
const size = ref(10);
const search = ref('');
const loading = ref(false);

const toast = ref<{ message: string, type: 'success'|'error' } | null>(null);

const columns = computed(() => {
  switch (entity.value) {
    case 'products': return ['ID', 'Image', 'Name', 'Price', 'Quantity', 'Actions'];
    case 'categories': return ['ID', 'Image', 'Name', 'Actions'];
    case 'brands': return ['ID', 'Logo', 'Name', 'Actions'];
    case 'orders': return ['ID', 'Total', 'Status', 'Actions'];
    case 'users': return ['ID', 'Avatar', 'Name', 'Email', 'Role', 'Actions'];
    case 'vouchers': return ['ID', 'Code', 'Discount', 'Actions'];
    case 'promotions': return ['ID', 'Name', 'Actions'];
    case 'banners': return ['ID', 'Image', 'Name', 'Actions'];
    case 'policies': return ['ID', 'Name', 'Actions'];
    case 'reviews': return ['ID', 'Rating', 'Actions'];
    default: return ['ID', 'Actions'];
  }
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await adminApi.list(entity.value, {
      page: currentPage.value,
      size: size.value,
      search: search.value || undefined
    });
    data.value = res.data.content;
    totalElements.value = res.data.totalElements;
    totalPages.value = res.data.totalPages;
  } catch (err) {
    console.error(err);
    toast.value = { message: 'Failed to load data', type: 'error' };
  } finally {
    loading.value = false;
  }
};

watch(() => route.params.entity, () => {
  currentPage.value = 0;
  search.value = '';
  loadData();
});

onMounted(() => {
  loadData();
});

const handleSearch = () => {
  currentPage.value = 0;
  loadData();
};

const handlePageChange = (page: number) => {
  currentPage.value = page - 1; // Backend uses 0-indexed pages
  loadData();
};

const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

const deleteId = ref<number | null>(null);

const confirmDelete = (id: number) => {
  deleteId.value = id;
};

const doDelete = async () => {
  if (deleteId.value === null) return;
  try {
    await adminApi.delete(entity.value, deleteId.value);
    toast.value = { message: 'Deleted successfully', type: 'success' };
    loadData();
  } catch (err) {
    toast.value = { message: 'Failed to delete', type: 'error' };
  } finally {
    deleteId.value = null;
  }
};
</script>

<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold capitalize">{{ entity }} List</h2>
      <button 
        @click="router.push(`/admin/${entity}/new`)" 
        class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded shadow text-sm font-medium"
      >
        Create New
      </button>
    </div>

    <div class="mb-4 flex gap-4">
      <input 
        v-model="search" 
        @keyup.enter="handleSearch"
        type="text" 
        placeholder="Search..." 
        class="border border-gray-300 rounded px-3 py-2 w-64 text-sm"
      />
      <button @click="handleSearch" class="bg-gray-100 hover:bg-gray-200 border border-gray-300 px-4 py-2 rounded text-sm font-medium">Search</button>
    </div>

    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div v-if="loading" class="p-8 flex justify-center">
        <LoadingSpinner />
      </div>
      <table v-else class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th v-for="col in columns" :key="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {{ col }}
            </th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          <tr v-for="row in data" :key="row.id" class="hover:bg-gray-50">
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ row.id }}</td>
            
            <template v-if="entity === 'products'">
              <td class="px-6 py-4 whitespace-nowrap"><img :src="row.images?.[0] || 'https://via.placeholder.com/40'" class="h-10 w-10 object-cover rounded" /></td>
              <td class="px-6 py-4 text-sm text-gray-900">{{ row.name }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ formatCurrency(row.price) }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ row.quantity || 0 }}</td>
            </template>
            
            <template v-else-if="entity === 'orders'">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ formatCurrency(row.totalAmount) }}</td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full"
                  :class="{
                    'bg-green-100 text-green-800': row.status === 'COMPLETED' || row.status === 'PAID',
                    'bg-yellow-100 text-yellow-800': row.status === 'PENDING' || row.status === 'UNPAID',
                    'bg-blue-100 text-blue-800': row.status === 'CONFIRMED',
                    'bg-indigo-100 text-indigo-800': row.status === 'SHIPPING',
                    'bg-red-100 text-red-800': row.status === 'CANCELLED' || row.status === 'FAILED'
                  }">
                  {{ row.status }}
                </span>
              </td>
            </template>
            
            <template v-else-if="entity === 'categories' || entity === 'brands'">
              <td class="px-6 py-4 whitespace-nowrap"><img :src="row.imageUrl || row.logoUrl || 'https://via.placeholder.com/40'" class="h-10 w-10 object-cover rounded" /></td>
              <td class="px-6 py-4 text-sm text-gray-900">{{ row.name }}</td>
            </template>
            
            <template v-else-if="entity === 'users'">
              <td class="px-6 py-4 whitespace-nowrap"><img :src="row.avatarUrl || 'https://via.placeholder.com/40'" class="h-10 w-10 rounded-full object-cover" /></td>
              <td class="px-6 py-4 text-sm text-gray-900">{{ row.firstName }} {{ row.lastName }}</td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ row.email }}</td>
              <td class="px-6 py-4 text-sm text-gray-900">{{ row.role }}</td>
            </template>
            
            <template v-else-if="entity === 'vouchers'">
              <td class="px-6 py-4 text-sm text-gray-900">{{ row.code }}</td>
              <td class="px-6 py-4 text-sm text-gray-900">{{ row.discountValue }} ({{ row.discountType }})</td>
            </template>
            
            <template v-else-if="entity === 'promotions' || entity === 'policies'">
              <td class="px-6 py-4 text-sm text-gray-900">{{ row.title || row.name }}</td>
            </template>
            
            <template v-else-if="entity === 'banners'">
              <td class="px-6 py-4 whitespace-nowrap"><img :src="row.imageUrl || 'https://via.placeholder.com/80x40'" class="h-10 object-cover rounded" /></td>
              <td class="px-6 py-4 text-sm text-gray-900">{{ row.title || row.name }}</td>
            </template>
            
            <template v-else-if="entity === 'reviews'">
              <td class="px-6 py-4 text-sm text-gray-900">{{ row.rating }} / 5</td>
            </template>
            
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-3">
              <button v-if="entity === 'orders'" @click="router.push(`/admin/orders/${row.id}`)" class="text-blue-600 hover:text-blue-900"><EyeIcon class="h-5 w-5 inline" /></button>
              <button v-else @click="router.push(`/admin/${entity}/${row.id}`)" class="text-indigo-600 hover:text-indigo-900"><PencilIcon class="h-5 w-5 inline" /></button>
              <button @click="confirmDelete(row.id)" class="text-red-600 hover:text-red-900"><TrashIcon class="h-5 w-5 inline" /></button>
            </td>
          </tr>
          <tr v-if="data.length === 0">
            <td :colspan="columns.length" class="px-6 py-10 text-center text-sm text-gray-500">No {{ entity }} found.</td>
          </tr>
        </tbody>
      </table>
      
      <div v-if="totalPages > 1" class="px-6 py-4 border-t border-gray-200">
        <Pagination 
          :current-page="currentPage + 1" 
          :total-pages="totalPages" 
          @page-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="deleteId !== null" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div class="bg-white rounded-lg p-6 max-w-sm w-full mx-4 shadow-xl">
        <h3 class="text-lg font-bold text-gray-900 mb-4">Confirm Delete</h3>
        <p class="text-sm text-gray-500 mb-6">Are you sure you want to delete this item? This action cannot be undone.</p>
        <div class="flex justify-end space-x-3">
          <button @click="deleteId = null" class="px-4 py-2 border border-gray-300 rounded text-sm font-medium text-gray-700 hover:bg-gray-50">Cancel</button>
          <button @click="doDelete" class="px-4 py-2 bg-red-600 text-white rounded text-sm font-medium hover:bg-red-700">Delete</button>
        </div>
      </div>
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
