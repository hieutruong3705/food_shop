<script setup>
import { ref, watch, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuth } from "./stores/auth.js";
import { api } from './services/api';
import ShopIcon from './components/ShopIcon.vue';
import CategoryMenu from './components/CategoryMenu.vue';
import TechChat from './components/TechChat.vue';
import AdminLayout from './components/AdminLayout.vue';
const auth = useAuth(), router = useRouter(), route = useRoute();
const search = ref(""), error = ref("");
const categories = ref([]), menuOpen = ref(false);
watch(() => route.fullPath, () => { menuOpen.value = false; });
watch(menuOpen, async open => { if(open) { try { categories.value = await api('/categories'); } catch { /* Keep the last loaded menu; the catalog exposes errors. */ } } });
onMounted(async () => { try { categories.value = await api('/categories'); } catch { /* The catalog page provides retry and errors. */ } });
function find() { router.push({ path: "/products", query: { search: search.value } }); }
async function logout() { try { await auth.logout(); router.push("/"); } catch (e) { error.value = e.message; } }
function expired() { auth.user = null; if (route.meta.auth) router.push({ path: "/login", query: { redirect: route.fullPath } }); }
onMounted(() => window.addEventListener("session-expired", expired));
onUnmounted(() => window.removeEventListener("session-expired", expired));
</script>
<template>
  <AdminLayout v-if="route.meta.admin"><RouterView /></AdminLayout>
  <template v-else>
  <div class="topbar"><span><ShopIcon name="truck" /> Miễn phí vận chuyển đơn từ 1 triệu</span><span><ShopIcon name="payment" /> Thanh toán khi nhận hàng</span><RouterLink to="/policies">Chính sách &amp; bảo hành <ShopIcon name="chevron" /></RouterLink></div>
  <header class="site-header">
    <RouterLink to="/" class="brand"><span class="brand-icon">t.</span><span>TechShop<small>CÔNG NGHỆ CHO MỖI NGÀY</small></span></RouterLink>
    <form class="header-search" @submit.prevent="find"><input v-model="search" placeholder="Hôm nay bạn đang tìm gì?" aria-label="Tìm kiếm sản phẩm" /><button aria-label="Tìm kiếm"><ShopIcon name="search" /></button></form>
    <nav>
      <RouterLink to="/cart" class="header-action"><ShopIcon name="cart" /><span>Giỏ hàng</span></RouterLink>
      <template v-if="auth.user"><RouterLink to="/orders">Đơn hàng</RouterLink><RouterLink to="/account" class="header-action"><ShopIcon name="user" />Tài khoản</RouterLink><RouterLink v-if="auth.admin" to="/admin">Quản trị</RouterLink><button class="text-button" @click="logout">Đăng xuất</button></template>
      <RouterLink v-else to="/login" class="header-action"><ShopIcon name="user" /><span>Đăng nhập ↗</span></RouterLink>
    </nav>
  </header>
  <div class="store-navigation">
    <div class="store-navigation-inner"><button class="category-toggle" :aria-expanded="menuOpen" aria-controls="header-categories" @click="menuOpen = !menuOpen"><ShopIcon name="menu" />Danh mục sản phẩm</button><RouterLink to="/products">Tất cả sản phẩm</RouterLink><RouterLink to="/products?sort=newest">Hàng mới về</RouterLink><RouterLink to="/products?sort=popular">Được quan tâm</RouterLink><RouterLink to="/policies">Hỗ trợ mua hàng</RouterLink></div>
    <CategoryMenu v-if="menuOpen" :categories="categories" @close="menuOpen = false" />
  </div>
  <p v-if="error" class="notice error" role="alert">{{ error }}</p>
  <main class="page-shell"><RouterView /></main>
  <TechChat />
  <footer>
    <div class="footer-main">
      <div><RouterLink to="/" class="brand">TechShop<span class="logo-dot">.</span></RouterLink><p>Lựa chọn công nghệ phù hợp.<br />Đồng hành cùng trải nghiệm của bạn.</p><span class="footer-payment">COD · Thanh toán khi nhận hàng</span></div>
      <div><strong>Khám phá</strong><RouterLink to="/products">Sản phẩm</RouterLink><RouterLink to="/orders">Đơn hàng của tôi</RouterLink></div>
      <div><strong>Hỗ trợ mua hàng</strong><RouterLink to="/policies">Chính sách &amp; bảo hành</RouterLink><RouterLink to="/account">Tài khoản</RouterLink><RouterLink to="/cart">Giỏ hàng của bạn</RouterLink></div>
      <div><strong>Mua sắm thuận tiện</strong><p>Xem rõ thông số, lựa chọn phiên bản và theo dõi đơn hàng ngay trên TechShop.</p><RouterLink to="/products" class="footer-explore">Khám phá ngay →</RouterLink></div>
    </div>
    <div class="footer-bottom"><span>© {{ new Date().getFullYear() }} TechShop</span><span>Được tạo cho những trải nghiệm mới.</span></div>
  </footer>
  </template>
</template>
