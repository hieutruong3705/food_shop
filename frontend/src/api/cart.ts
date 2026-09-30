import apiClient from './client';
import type { CartResponse } from '@/types';

export const cartApi = {
  getCart: () => apiClient.get<CartResponse>('/cart'),
  addItem: (data: { productId: number; variantId?: number; quantity: number }) => 
    apiClient.post<CartResponse>('/cart/items', data),
  updateItem: (itemId: number, quantity: number) => 
    apiClient.put<CartResponse>(`/cart/items/${itemId}`, { quantity }),
  removeItem: (itemId: number) => apiClient.delete<CartResponse>(`/cart/items/${itemId}`),
  checkout: (data: any) => apiClient.post('/cart/checkout', data),
  applyVoucher: (code: string) => apiClient.post<CartResponse>('/cart/voucher', { code }),
};
