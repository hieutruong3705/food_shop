import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { User } from '@/types';
import { authApi } from '@/api/auth';

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null);
  const loading = ref(false);

  const isLoggedIn = computed(() => !!user.value);
  const isAdmin = computed(() => user.value?.role === 'ADMIN');

  async function fetchMe() {
    try {
      loading.value = true;
      const response = await authApi.getMe();
      user.value = response.data;
    } catch (error) {
      user.value = null;
    } finally {
      loading.value = false;
    }
  }

  async function login(data: any) {
    const response = await authApi.login(data);
    user.value = response.data.user;
    return response;
  }

  async function logout() {
    await authApi.logout();
    user.value = null;
  }

  return {
    user,
    loading,
    isLoggedIn,
    isAdmin,
    fetchMe,
    login,
    logout
  };
});
