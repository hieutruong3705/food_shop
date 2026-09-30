import apiClient from './client';
import type { PageResponse } from '@/types';

export const adminApi = {
  list: (entity: string, params?: any) => apiClient.get<PageResponse<any>>(`/admin/${entity}`, { params }),
  get: (entity: string, id: number | string) => apiClient.get<any>(`/admin/${entity}/${id}`),
  save: (entity: string, data: any) => {
    if (data.id) {
      return apiClient.put(`/admin/${entity}/${data.id}`, data);
    }
    return apiClient.post(`/admin/${entity}`, data);
  },
  delete: (entity: string, id: number | string) => apiClient.delete(`/admin/${entity}/${id}`),
};
