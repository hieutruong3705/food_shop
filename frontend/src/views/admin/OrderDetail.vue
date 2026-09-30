<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminApi } from '@/api/admin';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import Toast from '@/components/Toast.vue';

const route = useRoute();
const router = useRouter();
const orderId = route.params.id as string;

const order = ref<any>(null);
const loading = ref(true);
const saving = ref(false);
const toast = ref<{ message: string, type: 'success'|'error' } | null>(null);

const statusOptions = ['PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED'];

const loadOrder = async () => {
  loading.value = true;
  try {
    const res = await adminApi.get('orders', orderId);
    order.value = res.data || res;
  } catch (err) {
    console.error(err);
    toast.value = { message: 'Failed to load order', type: 'error' };
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadOrder();
});

const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
};

const formatDate = (dateString: string) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleString('vi-VN');
};

const updateStatus = async () => {
  if (!order.value) return;
  saving.value = true;
  try {
    // API might expect { status } payload or similar
    await adminApi.save('orders', { id: order.value.id, status: order.value.status });
    toast.value = { message: 'Order status updated', type: 'success' };
  } catch (err) {
    console.error(err);
    toast.value = { message: 'Failed to update order status', type: 'error' };
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center space-x-4">
        <button @click="router.push('/admin/orders')" class="text-gray-500 hover:text-gray-700">
          &larr; Back to Orders
        </button>
        <h2 class="text-2xl font-bold text-gray-900">Order #{{ orderId }}</h2>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center p-12">
      <LoadingSpinner />
    </div>

    <div v-else-if="order" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      
      <!-- Main Info -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Status update card -->
        <div class="bg-white rounded-lg shadow p-6 border border-gray-200">
          <h3 class="text-lg font-medium text-gray-900 mb-4">Order Status</h3>
          <div class="flex items-end space-x-4">
            <div class="flex-1">
              <label class="block text-sm font-medium text-gray-700 mb-1">Current Status</label>
              <select v-model="order.status" class="block w-full border-gray-300 rounded-md shadow-sm py-2 px-3 focus:ring-blue-500 focus:border-blue-500 sm:text-sm">
                <option v-for="status in statusOptions" :key="status" :value="status">{{ status }}</option>
              </select>
            </div>
            <button @click="updateStatus" :disabled="saving" class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md shadow-sm text-sm font-medium disabled:opacity-50">
              {{ saving ? 'Saving...' : 'Update Status' }}
            </button>
          </div>
        </div>

        <!-- Order Items -->
        <div class="bg-white rounded-lg shadow overflow-hidden border border-gray-200">
          <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <h3 class="text-lg font-medium text-gray-900">Order Items</h3>
          </div>
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-white">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Product</th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Price</th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Qty</th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Total</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr v-for="(item, idx) in order.details || []" :key="idx">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ item.productName || `Product ID ${item.productId}` }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">{{ formatCurrency(item.price) }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">{{ item.quantity }}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium text-right">{{ formatCurrency(item.price * item.quantity) }}</td>
              </tr>
              <tr v-if="!order.details || order.details.length === 0">
                <td colspan="4" class="px-6 py-4 text-center text-sm text-gray-500">No items found in this order.</td>
              </tr>
            </tbody>
          </table>
          <div class="bg-gray-50 px-6 py-4 border-t border-gray-200 flex justify-end">
            <div class="text-right">
              <span class="text-sm font-medium text-gray-500 mr-4">Total Amount:</span>
              <span class="text-xl font-bold text-gray-900">{{ formatCurrency(order.totalAmount) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Sidebar -->
      <div class="space-y-6">
        <!-- Customer Info -->
        <div class="bg-white rounded-lg shadow p-6 border border-gray-200">
          <h3 class="text-lg font-medium text-gray-900 mb-4 border-b pb-2">Customer Info</h3>
          <div class="space-y-3 text-sm">
            <div>
              <span class="text-gray-500 block">User ID:</span>
              <span class="font-medium text-gray-900">{{ order.userId || 'N/A' }}</span>
            </div>
            <div>
              <span class="text-gray-500 block">Created At:</span>
              <span class="font-medium text-gray-900">{{ formatDate(order.createdAt) }}</span>
            </div>
          </div>
        </div>

        <!-- Payment Info -->
        <div class="bg-white rounded-lg shadow p-6 border border-gray-200">
          <h3 class="text-lg font-medium text-gray-900 mb-4 border-b pb-2">Payment Info</h3>
          <div class="space-y-3 text-sm">
            <div>
              <span class="text-gray-500 block">Payment Method:</span>
              <span class="font-medium text-gray-900 uppercase">{{ order.paymentMethod || 'UNKNOWN' }}</span>
            </div>
            <div>
              <span class="text-gray-500 block">Payment Status:</span>
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                :class="{
                  'bg-green-100 text-green-800': order.paymentStatus === 'PAID',
                  'bg-yellow-100 text-yellow-800': order.paymentStatus === 'UNPAID' || !order.paymentStatus,
                  'bg-red-100 text-red-800': order.paymentStatus === 'FAILED'
                }">
                {{ order.paymentStatus || 'UNPAID' }}
              </span>
            </div>
          </div>
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
