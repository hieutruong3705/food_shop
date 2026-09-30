import apiClient from './client';
import type { OrderResponse, PageResponse } from '@/types';

export const ordersApi = {
  getOrders: (params?: any) => apiClient.get<PageResponse<OrderResponse>>('/orders', { params }),
  getOrder: (id: number) => apiClient.get<OrderResponse>(`/orders/${id}`),
  cancelOrder: (id: number) => apiClient.post(`/orders/${id}/cancel`),
  downloadInvoice: (id: number) => apiClient.get(`/orders/${id}/invoice`, { responseType: 'blob' }),
};
