<script setup>
import { onMounted, ref, computed } from 'vue';
import { api } from '../services/api';
import { useTask, imageUrl, imageError, discountPercent } from '../lib/shop';
import { categoryImage } from '../lib/presentation';
import ProductCard from '../components/ProductCard.vue';
import TaskState from '../components/TaskState.vue';
import ShopIcon from '../components/ShopIcon.vue';
import HomeHero from '../components/HomeHero.vue';
const products = ref([]), newest = ref([]), categories = ref([]), banners = ref([]);
const selectedCategory = ref('');
const { busy, error, run } = useTask();
const filters = computed(() => [...new Set(products.value.map(p => p.category).filter(Boolean))]);
const shown = computed(() => selectedCategory.value ? products.value.filter(p => p.category === selectedCategory.value) : products.value);
const maxDiscount = computed(() => Math.max(0, ...products.value.map(p => Number(String(discountPercent(p.price, p.discountedPrice)).replace(',', '.')) || 0)));
onMounted(() => run(async () => {
  const [p, c, b, n] = await Promise.all([api('/products?size=8&sort=popular'), api('/categories'), api('/banners'), api('/products?size=8&sort=newest')]);
  products.value = p.content; categories.value = c; banners.value = b; newest.value = n.content;
}));
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
  <section class="retail-categories"><div class="retail-section-title"><h2>Bạn đang tìm gì?</h2><RouterLink to="/products">Tất cả danh mục <ShopIcon name="arrow" /></RouterLink></div><div class="retail-category-grid"><RouterLink v-for="c in categories" :key="c.id" :to="{ path: '/products', query: { categoryId: c.id } }"><span><img :src="c.image ? imageUrl(c.image) : categoryImage(c.name)" @error="event => event.target.src = categoryImage(c.name)" alt="" /></span><strong>{{ c.name }}</strong></RouterLink></div></section>
  <section class="featured-retail"><div class="featured-heading"><div><span class="campaign-tag"><ShopIcon name="bolt" /> GỢI Ý DÀNH CHO BẠN</span><h2>Thiết bị nổi bật <small v-if="maxDiscount > 0">· Giảm đến {{ maxDiscount }}%</small></h2></div><RouterLink to="/products?sort=popular">Xem tất cả <ShopIcon name="arrow" /></RouterLink></div><div class="retail-filter-tabs" aria-label="Lọc sản phẩm nổi bật"><button :aria-pressed="!selectedCategory" :class="{ active: !selectedCategory }" @click="selectedCategory = ''">Tất cả</button><button v-for="name in filters" :key="name" :aria-pressed="selectedCategory === name" :class="{ active: selectedCategory === name }" @click="selectedCategory = name">{{ name }}</button></div><div class="product-grid" data-testid="featured-products"><ProductCard v-for="p in shown" :key="p.id" :product="p" /></div><p v-if="!busy && !error && !shown.length" class="empty">Cửa hàng đang cập nhật sản phẩm.</p></section>
  <section class="retail-spotlight"><div><span class="campaign-tag">KHÔNG GIAN CỦA BẠN</span><h2>Một góc làm việc.<br />Nhiều điều có thể.</h2><p>Sẵn sàng cho ý tưởng tiếp theo với thiết bị phù hợp.</p><RouterLink to="/products" class="button">Thiết lập góc làm việc <ShopIcon name="arrow" /></RouterLink></div><img src="/tech/laptop.svg" alt="Minh họa laptop" /><img class="spotlight-mouse" src="/tech/mouse.svg" alt="Minh họa chuột" /></section>
  <section v-if="newest.length" class="retail-newest"><div class="retail-section-title"><h2>Sản phẩm mới</h2><RouterLink to="/products?sort=newest">Khám phá thêm <ShopIcon name="arrow" /></RouterLink></div><div class="product-grid"><ProductCard v-for="p in newest" :key="p.id" :product="p" /></div></section>
  <section v-if="banners.length" class="retail-banner-links"><a v-for="b in banners" :key="b.id" :href="b.link || '/products'"><img :src="imageUrl(b.imageUrl)" :alt="b.name" @error="imageError" /><div><small>BỘ SƯU TẬP TECHSHOP</small><h2>{{ b.name }}</h2><span>Khám phá ngay →</span></div></a></section>
</template>
