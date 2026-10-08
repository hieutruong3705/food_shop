<script setup>
import {ref,computed,watch,onMounted,onUnmounted} from 'vue';
import {imageUrl,imageError} from '../lib/shop';
import {categoryImage} from '../lib/presentation';
import ShopIcon from './ShopIcon.vue';
const props=defineProps({banners:{type:Array,default:()=>[]},categories:{type:Array,default:()=>[]}});
const index=ref(0), hovered=ref(false), reduced=ref(false);
const defaults=[{id:'laptop',name:'Nâng tầm trải nghiệm công nghệ',imageUrl:'/tech/laptop.svg'},{id:'phone',name:'Điện thoại cho mỗi ngày',imageUrl:'/tech/phone.svg'},{id:'headphones',name:'Thêm cảm hứng. Thêm trải nghiệm.',imageUrl:'/tech/headphones.svg'}];
const slides=computed(()=>props.banners.length?props.banners:defaults);
const current=computed(()=>slides.value[index.value]||slides.value[0]);
let timer;
function restart(){clearInterval(timer);if(slides.value.length>1&&!hovered.value&&!reduced.value&&!document.hidden)timer=setInterval(()=>index.value=(index.value+1)%slides.value.length,4500);}
function go(value){index.value=(value+slides.value.length)%slides.value.length;restart();}
function destination(slide){if(slide.link)return slide.link;const category=props.categories.find(c=>categoryImage(c.name)===slide.imageUrl);return category?'/products?categoryId='+category.id:'/products';}
watch([slides,hovered,reduced],()=>{if(index.value>=slides.value.length)index.value=0;restart();});
onMounted(()=>{reduced.value=window.matchMedia('(prefers-reduced-motion: reduce)').matches;document.addEventListener('visibilitychange',restart);restart();});
onUnmounted(()=>{clearInterval(timer);document.removeEventListener('visibilitychange',restart);});
</script>
<template>
  <section class="db-carousel" aria-label="Banner TechShop" aria-roledescription="carousel" @mouseenter="hovered=true" @mouseleave="hovered=false">
    <Transition name="banner-fade"><a :key="current.id" class="banner-slide" :href="destination(current)"><img v-if="current.imageUrl" :src="imageUrl(current.imageUrl)" :alt="current.name" @error="imageError" /><div v-else class="banner-placeholder"><ShopIcon name="bolt" /><span>{{ current.name }}</span></div><div v-if="!props.banners.length" class="banner-caption"><span>CÔNG NGHỆ CHO MỖI NGÀY</span><h1>{{ current.name }}</h1><b>Khám phá ngay →</b></div></a></Transition>
    <button class="carousel-arrow previous" :disabled="slides.length<2" aria-label="Banner trước" @click="go(index-1)">‹</button><button class="carousel-arrow next" :disabled="slides.length<2" aria-label="Banner tiếp theo" @click="go(index+1)">›</button><div class="carousel-controls"><div class="carousel-dots"><button v-for="(slide,i) in slides" :key="slide.id" :aria-label="`Xem banner ${i+1}: ${slide.name}`" :aria-pressed="index===i" :class="{active:index===i}" @click="go(i)"></button></div></div>
    <small class="carousel-count">{{ index+1 }} / {{ slides.length }}</small>
  </section>
</template>
