import apiClient from './client';
import type { User, LoginResponse } from '@/types';

export const authApi = {
  login: (data: any) => apiClient.post<LoginResponse>('/auth/login', data),
  register: (data: any) => apiClient.post('/auth/register', data),
  logout: () => apiClient.post('/auth/logout'),
  getMe: () => apiClient.get<User>('/auth/me'),
  updateProfile: (data: any) => apiClient.put<User>('/auth/profile', data),
  uploadAvatar: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post('/auth/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  forgotPassword: (email: string) => apiClient.post('/auth/forgot-password', { email }),
  resetPassword: (data: any) => apiClient.post('/auth/reset-password', data),
};
