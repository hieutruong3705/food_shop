import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/Home.vue') },
        { path: 'products', name: 'products', component: () => import('@/views/Products.vue') },
        { path: 'products/:slug', name: 'product-detail', component: () => import('@/views/ProductDetail.vue') },
        { path: 'policy/:id', name: 'policy', component: () => import('@/views/Policy.vue') },
        { 
          path: 'cart', 
          name: 'cart', 
          component: () => import('@/views/Cart.vue'),
          meta: { requiresAuth: true }
        },
        { 
          path: 'checkout', 
          name: 'checkout', 
          component: () => import('@/views/Checkout.vue'),
          meta: { requiresAuth: true }
        },
        { 
          path: 'orders', 
          name: 'orders', 
          component: () => import('@/views/Orders.vue'),
          meta: { requiresAuth: true }
        },
        { 
          path: 'orders/:id', 
          name: 'order-detail', 
          component: () => import('@/views/OrderDetail.vue'),
          meta: { requiresAuth: true }
        },
        { 
          path: 'account', 
          name: 'account', 
          component: () => import('@/views/Account.vue'),
          meta: { requiresAuth: true }
        },
      ]
    },
    {
      path: '/',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [
        { path: 'login', name: 'login', component: () => import('@/views/Login.vue') },
        { path: 'register', name: 'register', component: () => import('@/views/Register.vue') },
        { path: 'forgot-password', name: 'forgot-password', component: () => import('@/views/ForgotPassword.vue') },
        { path: 'reset-password', name: 'reset-password', component: () => import('@/views/ResetPassword.vue') },
      ]
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        { path: '', name: 'admin-dashboard', component: () => import('@/views/admin/Dashboard.vue') },
        { path: ':entity', name: 'admin-list', component: () => import('@/views/admin/EntityList.vue') },
        { path: ':entity/new', name: 'admin-create', component: () => import('@/views/admin/EntityForm.vue') },
        { path: ':entity/:id', name: 'admin-edit', component: () => import('@/views/admin/EntityForm.vue') },
      ]
    }
  ]
});

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();
  
  // Need to fetch user on first load if we have a session
  if (!authStore.user && !authStore.loading) {
    await authStore.fetchMe();
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'login', query: { redirect: to.fullPath } });
  } else if (to.meta.requiresAdmin && (!authStore.isLoggedIn || !authStore.isAdmin)) {
    next({ name: 'home' });
  } else {
    next();
  }
});

export default router;
