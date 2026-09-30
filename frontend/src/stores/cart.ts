import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { CartResponse } from '@/types';
import { cartApi } from '@/api/cart';

export const useCartStore = defineStore('cart', () => {
  const cart = ref<CartResponse | null>(null);
  const loading = ref(false);

  const itemCount = computed(() => {
    if (!cart.value) return 0;
    return cart.value.items.reduce((total, item) => total + item.quantity, 0);
  });

  const totalPrice = computed(() => cart.value?.totalPrice || 0);

  async function fetchCart() {
    try {
      loading.value = true;
      const response = await cartApi.getCart();
      cart.value = response.data;
    } catch (error) {
      console.error('Failed to fetch cart', error);
    } finally {
      loading.value = false;
    }
  }

  async function addItem(productId: number, quantity: number, variantId?: number) {
    const response = await cartApi.addItem({ productId, variantId, quantity });
    cart.value = response.data;
  }

  async function updateItem(itemId: number, quantity: number) {
    const response = await cartApi.updateItem(itemId, quantity);
    cart.value = response.data;
  }

  async function removeItem(itemId: number) {
    const response = await cartApi.removeItem(itemId);
    cart.value = response.data;
  }

  async function checkout(data: any) {
    await cartApi.checkout(data);
    await fetchCart(); // Refresh cart after checkout
  }

  return {
    cart,
    loading,
    itemCount,
    totalPrice,
    fetchCart,
    addItem,
    updateItem,
    removeItem,
    checkout
  };
});
