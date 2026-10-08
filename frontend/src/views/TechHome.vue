<script setup>
import { onMounted, onUnmounted, ref, computed, watch } from 'vue';
import { api } from '../services/api';
import { useTask, imageUrl, imageError, discountPercent } from '../lib/shop';
import { categoryImage } from '../lib/presentation';
import ProductCard from '../components/ProductCard.vue';
import TaskState from '../components/TaskState.vue';
import ShopIcon from '../components/ShopIcon.vue';
import HomeHero from '../components/HomeHero.vue';
import DealBoard from '../components/home/DealBoard.vue';
import GroupBanner from '../components/home/GroupBanner.vue';
import ProductCarousel from '../components/home/ProductCarousel.vue';
import ReviewStrip from '../components/home/ReviewStrip.vue';
import RecentlyViewed from '../components/home/RecentlyViewed.vue';
const products = ref([]), newest = ref([]), categories = ref([]), banners = ref([]), banner1 = ref(null), banner2 = ref(null), banner3 = ref(null), brandBanners = ref([]), groupBanners = ref([]), home = ref(null);
const { busy, error, run } = useTask();
const maxDiscount = computed(() => Math.max(0, ...products.value.map(p => Number(String(discountPercent(p.price, p.discountedPrice)).replace(',', '.')) || 0)));
const brandPlacements = ['HOME_BRAND_1_1', 'HOME_BRAND_1_2', 'HOME_BRAND_1_3', 'HOME_BRAND_1_4'];
// One advertising banner per home product group, and how many product cells each of them takes.
const groupPlacements = ['HOME_GROUP_1', 'HOME_GROUP_2', 'HOME_GROUP_3', 'HOME_GROUP_4', 'HOME_GROUP_5'];
const GROUP_BANNER_CELLS = [2, 1, 2, 1, 2];
onMounted(() => run(async () => {
  // The extra home blocks are optional: if /home fails the rest of the page still shows.
  const [p, c, b, b1, b2, b3, brands, groups, n, h] = await Promise.all([api('/products?size=8&sort=popular'), api('/categories'), api('/banners'), api('/banners?placement=HOME_BANNER_1').catch(() => []), api('/banners?placement=HOME_BANNER_2').catch(() => []), api('/banners?placement=HOME_BANNER_3').catch(() => []), Promise.all(brandPlacements.map(x => api(`/banners?placement=${x}`).catch(() => []))), Promise.all(groupPlacements.map(x => api(`/banners?placement=${x}`).catch(() => []))), api('/products?size=24&sort=newest'), api('/home').catch(() => null)]);
  products.value = p.content; categories.value = c; banners.value = b; banner1.value = Array.isArray(b1) ? b1[0] : null; banner2.value = Array.isArray(b2) ? b2[0] : null; banner3.value = Array.isArray(b3) ? b3[0] : null; brandBanners.value = brands.flatMap(row => Array.isArray(row) ? row.slice(0, 1) : []).filter(item => item?.imageUrl).slice(0, 4); groupBanners.value = groups.map(row => (Array.isArray(row) && row[0]?.imageUrl ? row[0] : null)); newest.value = n.content; home.value = h;
}));
// Sản phẩm mới gom theo danh mục: mỗi danh mục lấy đúng 10 sản phẩm để lấp kín 2 hàng,
// nhờ vậy khối không còn một hàng cụt ở cuối và không bị trùng một loại hàng.
const shuffle = (list) => {
  const out = [...list];
  for (let i = out.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [out[i], out[j]] = [out[j], out[i]]; }
  return out;
};
// Thiết bị nổi bật: bốc ngẫu nhiên trên toàn bộ kho hàng (không bó hẹp một loại),
// và tự đổi sang nhóm sản phẩm khác sau mỗi 5 phút.
// Ảnh minh hoạ khối "Một góc làm việc" lấy từ hàng thật trong kho thay cho hình vẽ mẫu.
const spotlight = ref({ main: '', side: '' });
async function loadSpotlight(list) {
  const pick = async (name) => {
    const cat = list.find((c) => c.name === name);
    if (!cat) return '';
    try {
      const page = await api(`/products?size=6&categoryId=${cat.id}&sort=popular`);
      return (page.content || []).find((x) => String(x.image).startsWith('/uploads/'))?.image || '';
    } catch { return ''; }
  };
  spotlight.value = { main: await pick('Laptop'), side: await pick('Màn hình') };
}
watch(categories, (list) => { if (list?.length) loadSpotlight(list); }, { immediate: true });
const FEATURED_REFRESH = 5 * 60 * 1000, FEATURED_SIZE = 20, PAGE_SIZE = 24;
const featured = ref([]);
let featuredTimer;
async function loadFeatured() {
  try {
    const first = await api(`/products?size=${PAGE_SIZE}&sort=newest`);
    const totalPages = Math.max(1, first.totalPages || 1);
    const wanted = Math.min(totalPages, 4);
    const picked = new Set([Math.floor(Math.random() * totalPages)]);
    while (picked.size < wanted) picked.add(Math.floor(Math.random() * totalPages));
    const pages = await Promise.all([...picked].map((n) => api(`/products?size=${PAGE_SIZE}&page=${n}&sort=newest`).catch(() => null)));
    const seen = new Set(), pool = [];
    pages.filter(Boolean).flatMap((x) => x.content || []).forEach((item) => {
      if (!seen.has(item.id)) { seen.add(item.id); pool.push(item); }
    });
    featured.value = shuffle(pool.length ? pool : first.content || []).slice(0, FEATURED_SIZE);
  } catch { /* giữ nguyên danh sách cũ nếu lần làm mới thất bại */ }
}
onMounted(() => { loadFeatured(); featuredTimer = setInterval(loadFeatured, FEATURED_REFRESH); });
onUnmounted(() => clearInterval(featuredTimer));
const GROUPS = 5, PER_GROUP = 10;
const newestByCategory = ref([]);
// A group that has a banner gives it the first cells of its first row and shows that many fewer products,
// so every row stays full.
const newestGroups = computed(() => newestByCategory.value.map((row, index) => {
  const banner = groupBanners.value[index] || null, cells = banner ? GROUP_BANNER_CELLS[index] : 0;
  return { ...row, banner, cells, products: row.products.slice(0, PER_GROUP - cells) };
}));
async function loadNewestGroups(list) {
  const chosen = [...list].sort((a, b) => (b.productCount || 0) - (a.productCount || 0)).slice(0, GROUPS);
  const rows = await Promise.all(chosen.map(async (category) => {
    try {
      const page = await api(`/products?size=${PER_GROUP}&sort=newest&categoryId=${category.id}`);
      return { category, products: (page.content || []).slice(0, PER_GROUP) };
    } catch { return { category, products: [] }; }
  }));
  newestByCategory.value = rows.filter((r) => r.products.length >= PER_GROUP / 2);
}
watch(categories, (list) => { if (list?.length) loadNewestGroups(list); }, { immediate: true });
</script>
<template>
  <div class="retail-hero-grid">
    <aside class="catalog-sidebar">
      <h2><ShopIcon name="grid" /> Danh mục sản phẩm</h2>
      <RouterLink v-for="c in categories" :key="c.id" :to="{ path: '/products', query: { categoryId: c.id } }"><img :src="c.image ? imageUrl(c.image) : categoryImage(c.name)" @error="event => event.target.src = categoryImage(c.name)" alt="" /><span>{{ c.name }}</span><ShopIcon name="chevron" /></RouterLink>
      <RouterLink to="/products" class="sidebar-all">Khám phá tất cả <ShopIcon name="arrow" /></RouterLink>
    </aside>
    <HomeHero :banners="banners" :categories="categories" />
  </div>
  <div class="retail-benefits"><div><ShopIcon name="truck" /><span>Giao hàng tận nơi<small>Miễn phí đơn từ 1 triệu</small></span></div><div><ShopIcon name="payment" /><span>Thanh toán COD<small>Thanh toán khi nhận hàng</small></span></div><div><ShopIcon name="grid" /><span>Lựa chọn linh hoạt<small>Màu sắc & phiên bản</small></span></div><RouterLink to="/policies"><ShopIcon name="shield" /><span>Mua sắm an tâm<small>Xem chính sách & bảo hành</small></span></RouterLink></div>
  <TaskState :busy="busy" :error="error" />
  <section v-if="banner1?.imageUrl" class="home-banner-one" aria-label="Banner 1"><a v-if="banner1.link" :href="banner1.link"><img :src="imageUrl(banner1.imageUrl)" :alt="banner1.name" @error="imageError" /></a><img v-else :src="imageUrl(banner1.imageUrl)" :alt="banner1.name" @error="imageError" /></section>
  <section class="featured-retail">
    <div class="featured-heading">
      <div><span class="campaign-tag"><ShopIcon name="bolt" /> KHÁCH HÀNG MUA NHIỀU NHẤT</span>
        <h2>Thiết bị nổi bật <small v-if="maxDiscount > 0">· Giảm đến {{ maxDiscount }}%</small></h2></div>
      <RouterLink to="/products?sort=popular">Xem tất cả <ShopIcon name="arrow" /></RouterLink>
    </div>
    <div data-testid="featured-products">
      <ProductCarousel :items="featured" :per-view="5" :interval="6000" label="Thiết bị nổi bật" />
    </div>
    <p v-if="!busy && !error && !featured.length" class="empty">Cửa hàng đang cập nhật sản phẩm.</p>
  </section>
  <section class="retail-categories"><div class="retail-section-title"><h2>Bạn đang tìm gì?</h2><RouterLink to="/products">Tất cả danh mục <ShopIcon name="arrow" /></RouterLink></div><div class="retail-category-grid"><RouterLink v-for="c in categories" :key="c.id" :to="{ path: '/products', query: { categoryId: c.id } }"><span><img :src="c.image ? imageUrl(c.image) : categoryImage(c.name)" @error="event => event.target.src = categoryImage(c.name)" alt="" /></span><strong>{{ c.name }}</strong></RouterLink></div></section>
  <DealBoard />
  <section v-if="newestGroups.length" class="retail-newest">
    <div class="retail-section-title"><h2>Sản phẩm mới</h2><RouterLink to="/products?sort=newest">Khám phá thêm <ShopIcon name="arrow" /></RouterLink></div>
    <div v-for="row in newestGroups" :key="row.category.id" class="newest-group">
      <div class="newest-group-head">
        <h3>{{ row.category.name }}</h3>
        <RouterLink :to="{ path: '/products', query: { categoryId: row.category.id, sort: 'newest' } }">Xem tất cả {{ row.category.name.toLowerCase() }} <ShopIcon name="arrow" /></RouterLink>
      </div>
      <div class="product-grid"><GroupBanner v-if="row.banner" :banner="row.banner" :cells="row.cells" /><ProductCard v-for="p in row.products" :key="p.id" :product="p" /></div>
    </div>
  </section>
  <section class="retail-spotlight"><div><span class="campaign-tag">KHÔNG GIAN CỦA BẠN</span><h2>Một góc làm việc.<br />Nhiều điều có thể.</h2><p>Sẵn sàng cho ý tưởng tiếp theo với thiết bị phù hợp.</p><RouterLink to="/products" class="button">Thiết lập góc làm việc <ShopIcon name="arrow" /></RouterLink></div><img v-if="spotlight.main" :src="imageUrl(spotlight.main)" alt="Laptop làm việc" @error="imageError" /><img v-if="spotlight.side" class="spotlight-mouse" :src="imageUrl(spotlight.side)" alt="Màn hình rời" @error="imageError" /></section>
  <RecentlyViewed />
  <section v-if="brandBanners.length" class="brand-specials" aria-labelledby="brand-specials-title"><h2 id="brand-specials-title">CHUYÊN TRANG THƯƠNG HIỆU</h2><div><a v-for="b in brandBanners" :key="b.id" :href="b.link || '/products'"><img :src="imageUrl(b.imageUrl)" :alt="b.name" @error="imageError" /></a></div></section>
  <section v-if="banner2?.imageUrl || banner3?.imageUrl" class="home-banner-pair" aria-label="Banner 2 và Banner 3"><a v-if="banner2?.imageUrl && banner2.link" :href="banner2.link"><img :src="imageUrl(banner2.imageUrl)" :alt="banner2.name" @error="imageError" /></a><img v-else-if="banner2?.imageUrl" :src="imageUrl(banner2.imageUrl)" :alt="banner2.name" @error="imageError" /><a v-if="banner3?.imageUrl && banner3.link" :href="banner3.link"><img :src="imageUrl(banner3.imageUrl)" :alt="banner3.name" @error="imageError" /></a><img v-else-if="banner3?.imageUrl" :src="imageUrl(banner3.imageUrl)" :alt="banner3.name" @error="imageError" /></section>
  <ReviewStrip :reviews="home?.reviews || []" />
  <section v-if="banners.length" class="retail-banner-links"><a v-for="b in banners" :key="b.id" :href="b.link || '/products'"><img v-if="b.imageUrl" :src="imageUrl(b.imageUrl)" :alt="b.name" @error="imageError" /><div v-else class="banner-link-placeholder"><ShopIcon name="bolt" /></div><div><small>BỘ SƯU TẬP TECHSHOP</small><h2>{{ b.name }}</h2><span>Khám phá ngay →</span></div></a></section>
</template>
