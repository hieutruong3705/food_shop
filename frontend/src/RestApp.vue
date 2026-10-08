<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuth } from "./stores/auth.js";
import { api } from './services/api';
import { loadOffers } from './lib/offers';
import ShopIcon from './components/ShopIcon.vue';
import CategoryMenu from './components/CategoryMenu.vue';
import TechChat from './components/TechChat.vue';
import AdminLayout from './components/AdminLayout.vue';
import BackToTop from './components/BackToTop.vue';
const auth = useAuth(), router = useRouter(), route = useRoute();
const search = ref(""), error = ref(""), newsletterEmail = ref(""), newsletterStatus = ref("");
const categories = ref([]), menuOpen = ref(false), accountOpen = ref(false);
// The top bar links to the running promotion that still has products to buy, and to the voucher page.
const offers = ref({ promotions: [], vouchers: [] });
const topPromotion = computed(() => offers.value.promotions.find(p => p.hasAvailableProducts) || null);
const topVoucher = computed(() => offers.value.vouchers[0] || null);
onMounted(async () => { try { offers.value = await loadOffers(); } catch { /* The top bar simply shows no offer. */ } });
const toTop = () => window.scrollTo({ top: 0, behavior: 'smooth' });
watch(() => route.fullPath, () => { menuOpen.value = false; accountOpen.value = false; });
watch(menuOpen, async open => { if(open) { try { categories.value = await api('/categories'); } catch { /* Keep the last loaded menu; the catalog exposes errors. */ } } });
onMounted(async () => { try { categories.value = await api('/categories'); } catch { /* The catalog page provides retry and errors. */ } });
function find() { router.push({ path: "/products", query: { search: search.value } }); }
async function logout() { try { await auth.logout(); router.push("/"); } catch (e) { error.value = e.message; } }
function subscribeNewsletter() { const valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newsletterEmail.value.trim()); newsletterStatus.value = valid ? "Cảm ơn bạn đã đăng ký nhận tin TechShop." : "Vui lòng nhập email hợp lệ."; if (valid) newsletterEmail.value = ""; }
function closeMenus(event) { if (!event.target.closest('.account-menu')) accountOpen.value = false; }
function escapeMenus(event) { if (event.key === 'Escape') accountOpen.value = false; }
onMounted(() => { document.addEventListener('click', closeMenus); document.addEventListener('keydown', escapeMenus); });
onUnmounted(() => { document.removeEventListener('click', closeMenus); document.removeEventListener('keydown', escapeMenus); });
function expired() { auth.user = null; if (route.meta.auth) router.push({ path: "/login", query: { redirect: route.fullPath } }); }
onMounted(() => window.addEventListener("session-expired", expired));
onUnmounted(() => window.removeEventListener("session-expired", expired));
</script>
<template>
  <AdminLayout v-if="route.meta.admin"><RouterView /></AdminLayout>
  <template v-else>
  <div class="site-top">
  <div class="topbar"><span><ShopIcon name="truck" /> Miễn phí vận chuyển toàn quốc</span><span><ShopIcon name="payment" /> Thanh toán khi nhận hàng</span><RouterLink v-if="topPromotion" class="topbar-offer promotion" :to="{ path: '/products', query: { promotionId: topPromotion.id } }"><ShopIcon name="bolt" /> Chương trình khuyến mại</RouterLink><RouterLink v-if="topVoucher" class="topbar-offer voucher" to="/vouchers"><ShopIcon name="ticket" /> Mã giảm giá</RouterLink><RouterLink to="/policies">Chính sách &amp; bảo hành <ShopIcon name="chevron" /></RouterLink></div>
  <header class="site-header">
    <RouterLink to="/" class="brand"><span class="brand-icon">t.</span><span>TechShop<small>CÔNG NGHỆ CHO MỖI NGÀY</small></span></RouterLink>
    <button class="category-toggle" :aria-expanded="menuOpen" aria-controls="header-categories" @click="menuOpen = !menuOpen"><ShopIcon name="menu" />Danh mục sản phẩm</button>
    <form class="header-search" @submit.prevent="find"><input v-model="search" placeholder="Hôm nay bạn đang tìm gì?" aria-label="Tìm kiếm sản phẩm" /><button aria-label="Tìm kiếm"><ShopIcon name="search" /></button></form>
    <nav>
      <RouterLink to="/cart" class="header-action"><ShopIcon name="cart" /><span>Giỏ hàng</span></RouterLink>
      <div v-if="auth.user" class="account-menu">
        <button class="header-action account-trigger" :aria-expanded="accountOpen" aria-haspopup="menu" @click="accountOpen = !accountOpen"><ShopIcon name="user" /><span>Cá nhân</span><i aria-hidden="true">▾</i></button>
        <div v-if="accountOpen" class="account-dropdown" role="menu">
          <RouterLink to="/account" role="menuitem">Thông tin cá nhân</RouterLink>
          <RouterLink to="/orders" role="menuitem">Đơn hàng</RouterLink>
          <RouterLink v-if="auth.admin" to="/admin" role="menuitem">Quản trị</RouterLink>
          <button role="menuitem" @click="logout">Đăng xuất</button>
        </div>
      </div>
      <RouterLink v-else to="/login" class="header-action"><ShopIcon name="user" /><span>Đăng nhập ↗</span></RouterLink>
    </nav>
  </header>
  <CategoryMenu v-if="menuOpen" :categories="categories" @close="menuOpen = false" />
  </div>
  <p v-if="error" class="notice error" role="alert">{{ error }}</p>
  <main class="page-shell"><RouterView /></main>
  <TechChat />
  <footer>
    <section class="footer-newsletter" aria-labelledby="newsletter-title">
      <div><h2 id="newsletter-title">Đăng ký nhận tin</h2><p>Nhận ưu đãi mới, mã giảm giá và gợi ý thiết bị phù hợp từ TechShop.</p></div>
      <form @submit.prevent="subscribeNewsletter"><input v-model="newsletterEmail" type="text" inputmode="email" placeholder="Email của bạn" aria-label="Email đăng ký nhận tin" /><button>Đăng ký</button><span role="status">{{ newsletterStatus }}</span></form>
    </section>
    <nav class="footer-linkbar" aria-label="Liên kết nhanh"><RouterLink to="/products">Sản phẩm</RouterLink><RouterLink to="/policies">Chính sách</RouterLink><RouterLink to="/orders">Đơn hàng</RouterLink><RouterLink to="/account">Tài khoản</RouterLink><RouterLink to="/cart">Giỏ hàng</RouterLink></nav>
    <div class="footer-main">
      <div><strong>Liên hệ TechShop</strong><p>Đại học Phenikaa, Yên Nghĩa, Hà Đông, Hà Nội</p><p>Hotline: 1900 xxxx<br />Email: support@techshop.local</p><div class="footer-socials" aria-label="Kênh TechShop"><span><ShopIcon name="grid" /></span><span><ShopIcon name="shield" /></span><span><ShopIcon name="payment" /></span></div></div>
      <div v-if="categories.length"><strong>Danh mục</strong><RouterLink v-for="c in categories.slice(0, 6)" :key="c.id" :to="{ path: '/products', query: { categoryId: c.id } }">{{ c.name }}</RouterLink></div>
      <div><strong>Dịch vụ khách hàng</strong><RouterLink to="/policies">Chính sách &amp; bảo hành</RouterLink><RouterLink to="/orders">Tra cứu đơn hàng</RouterLink><RouterLink to="/cart">Giỏ hàng của bạn</RouterLink></div>
      <div><strong>Thông tin</strong><RouterLink to="/products">Tất cả sản phẩm</RouterLink><RouterLink to="/products?sort=newest">Hàng mới về</RouterLink><RouterLink to="/products?sort=popular">Được quan tâm</RouterLink><RouterLink to="/account">Tài khoản</RouterLink></div>
    </div>
    <div class="footer-bottom"><span>© {{ new Date().getFullYear() }} TechShop. Được tạo cho những trải nghiệm mới.</span><div class="footer-badges" aria-label="Phương thức thanh toán"><span>COD</span><span>VNPay</span></div><a href="#top" class="footer-top" @click.prevent="toTop">Về đầu trang ↑</a></div>
  </footer>
  </template>
  <BackToTop :lifted="!route.meta.admin" />
</template>
