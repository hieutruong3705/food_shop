import { createRouter, createWebHistory } from "vue-router";
import { useAuth } from "../stores/auth";
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: "/", component: () => import("../views/TechHome.vue") },
    { path: "/products", component: () => import("../views/CatalogPage.vue") },
    {
      path: "/products/:id",
      component: () => import("../views/ProductPage.vue"),
    },
    ...["login", "register", "forgot-password", "reset-password", "verify"].map(
      (p) => ({
        path: "/" + p,
        component: () => import("../views/AuthPage.vue"),
      }),
    ),
    {
      path: "/cart",
      component: () => import("../views/CartPage.vue"),
      meta: { auth: true },
    },
    {
      path: "/checkout",
      component: () => import("../views/CheckoutPage.vue"),
      meta: { auth: true },
    },
    {
      path: "/orders/:id?",
      component: () => import("../views/OrdersPage.vue"),
      meta: { auth: true },
    },
    {
      path: "/account",
      component: () => import("../views/AccountPage.vue"),
      meta: { auth: true },
    },
    {
      path: "/payment-return",
      component: () => import("../views/PaymentReturnPage.vue"),
    },
    {
      path: "/vouchers",
      component: () => import("../views/VoucherPage.vue"),
    },
    {
      path: "/forbidden",
      component: () => import("../views/ForbiddenPage.vue"),
    },
    { path: "/policies", component: () => import("../views/PolicyPage.vue") },
    {
      path: "/admin/:resource?",
      component: () => import("../views/AdminPage.vue"),
      meta: { auth: true, admin: true },
    },
    {
      path: "/:pathMatch(.*)*",
      component: () => import("../views/NotFoundPage.vue"),
    },
  ],
});
router.beforeEach(async (to) => {
  const auth = useAuth();
  if (!auth.loaded) {
    try {
      await auth.load();
    } catch {
      auth.loaded = true;
    }
  }
  if (to.meta.auth && !auth.user)
    return { path: "/login", query: { redirect: to.fullPath } };
  if (to.meta.admin && !auth.admin) return "/forbidden";
});
export default router;
