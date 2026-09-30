import apiClient from './client';
import type { Product, Category, Brand, Banner, Policy, PageResponse } from '@/types';

export const productsApi = {
  getProducts: (params?: any) => apiClient.get<PageResponse<Product>>('/products', { params }),
  getProduct: (slug: string) => apiClient.get<Product>(`/products/${slug}`),
  getCategories: () => apiClient.get<Category[]>('/categories'),
  getBrands: () => apiClient.get<Brand[]>('/brands'),
  getBanners: () => apiClient.get<Banner[]>('/banners'),
  getPolicies: () => apiClient.get<Policy[]>('/policies'),
};
