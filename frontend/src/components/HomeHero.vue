<script setup>
import {ref,computed,watch,onMounted,onUnmounted} from 'vue';
import {api} from '../services/api';
import {imageUrl,money,date} from '../lib/shop';
import {categoryImage} from '../lib/presentation';
import OfferCarousel from './OfferCarousel.vue';
const props=defineProps({banners:{type:Array,default:()=>[]},categories:{type:Array,default:()=>[]}});
const index=ref(0), hovered=ref(false), reduced=ref(false), copied=ref(''), offers=ref({promotions:[],vouchers:[]}), error=ref('');
const defaults=[{id:'laptop',name:'Nâng tầm trải nghiệm công nghệ',imageUrl:'/tech/laptop.svg'},{id:'phone',name:'Điện thoại cho mỗi ngày',imageUrl:'/tech/phone.svg'},{id:'headphones',name:'Thêm cảm hứng. Thêm trải nghiệm.',imageUrl:'/tech/headphones.svg'}];
const slides=computed(()=>props.banners.length?props.banners:defaults);
const current=computed(()=>slides.value[index.value]||slides.value[0]);
let timer;
function restart(){clearInterval(timer);if(slides.value.length>1&&!hovered.value&&!reduced.value&&!document.hidden)timer=setInterval(()=>index.value=(index.value+1)%slides.value.length,4500);}
function go(value){index.value=(value+slides.value.length)%slides.value.length;restart();}
function destination(slide){if(slide.link)return slide.link;const category=props.categories.find(c=>categoryImage(c.name)===slide.imageUrl);return category?'/products?categoryId='+category.id:'/products';}
async function copy(code){try{await navigator.clipboard.writeText(code);copied.value='Đã sao chép mã '+code;}catch{copied.value='Bạn có thể sao chép mã hiển thị bên trên.';}}
async function loadOffers(){try{offers.value=await api('/offers');error.value='';}catch(e){error.value=e.message;}}
watch([slides,hovered,reduced],()=>{if(index.value>=slides.value.length)index.value=0;restart();});
onMounted(()=>{reduced.value=window.matchMedia('(prefers-reduced-motion: reduce)').matches;document.addEventListener('visibilitychange',restart);restart();loadOffers();});
onUnmounted(()=>{clearInterval(timer);document.removeEventListener('visibilitychange',restart);});
</script>
<template>
  <section class="db-carousel" aria-label="Banner TechShop" aria-roledescription="carousel" @mouseenter="hovered=true" @mouseleave="hovered=false">
    <Transition name="banner-fade"><a :key="current.id" class="banner-slide" :href="destination(current)"><img :src="imageUrl(current.imageUrl)" :alt="current.name" @error="event=>{if(!event.target.dataset.fallback){event.target.dataset.fallback='1';event.target.src='/tech/laptop.svg'}}" /><div class="banner-caption"><span>{{ props.banners.length?'BỘ SƯU TẬP TECHSHOP':'CÔNG NGHỆ CHO MỖI NGÀY' }}</span><h1>{{ current.name }}</h1><b>Khám phá ngay →</b></div></a></Transition>
    <button class="carousel-arrow previous" :disabled="slides.length<2" aria-label="Banner trước" @click="go(index-1)">‹</button><button class="carousel-arrow next" :disabled="slides.length<2" aria-label="Banner tiếp theo" @click="go(index+1)">›</button><div class="carousel-controls"><div class="carousel-dots"><button v-for="(slide,i) in slides" :key="slide.id" :aria-label="`Xem banner ${i+1}: ${slide.name}`" :aria-pressed="index===i" :class="{active:index===i}" @click="go(i)"></button></div></div>
    <small class="carousel-count">{{ index+1 }} / {{ slides.length }}</small>
  </section>
  <div class="hero-offers">
    <OfferCarousel class="promotion-card" label="Khuyến mãi đang diễn ra" :items="offers.promotions">
      <template #default="{item:promotion}">
        <strong class="offer-percent">Giảm {{ promotion.discountRate }}%</strong><h2>{{ promotion.name }}</h2><p>{{ promotion.description }}</p><small v-if="promotion.endDate">Đến {{ date(promotion.endDate) }}</small>
        <RouterLink v-if="promotion.hasAvailableProducts" :to="{path:'/products',query:{promotionId:promotion.id}}">Xem sản phẩm áp dụng →</RouterLink>
        <p v-else class="offer-unavailable">Sản phẩm áp dụng hiện chưa được mở bán.</p>
      </template>
      <template #empty><h2>Ưu đãi từ TechShop</h2><p>{{ error?'Chưa tải được chương trình.':'Hiện chưa có chương trình khuyến mãi đang áp dụng.' }}</p><RouterLink to="/products">Khám phá sản phẩm →</RouterLink></template>
      <template #footer><button v-if="error" class="text-button" @click="loadOffers">Thử lại</button></template>
    </OfferCarousel>
    <OfferCarousel class="voucher-card" label="Mã giảm giá" item-key="code" :items="offers.vouchers" @change="copied=''">
      <template #default="{item:voucher}">
        <strong class="offer-percent">Giảm {{ voucher.discount }}%</strong><button class="voucher-code" :aria-label="`Sao chép mã ${voucher.code}`" @click="copy(voucher.code)">{{ voucher.code }} <span>⧉</span></button><small>Đơn từ {{ money(voucher.minimumOrder) }}<template v-if="voucher.maximumDiscount"> · Giảm tối đa {{ money(voucher.maximumDiscount) }}</template></small>
        <details><summary>Điều kiện áp dụng</summary><p>{{ voucher.scopeDescription }}<br /><template v-if="voucher.endDate">Hạn dùng: {{ date(voucher.endDate) }}.</template> Kiểm tra lại điều kiện khi đặt hàng.</p></details><span v-if="copied" role="status">{{ copied }}</span>
      </template>
      <template #empty><h2>Mã giảm giá TechShop</h2><p>{{ error?'Chưa tải được mã giảm giá.':'Chưa có mã giảm giá còn hiệu lực. Hãy quay lại sau nhé.' }}</p></template>
      <template #footer><button v-if="error" class="text-button" @click="loadOffers">Thử lại</button></template>
    </OfferCarousel>
  </div>
</template>
