<script setup>
import {computed,onMounted,onUnmounted,ref,watch} from 'vue';
const props=defineProps({items:{type:Array,default:()=>[]},label:{type:String,required:true},itemKey:{type:String,default:'id'}});
const emit=defineEmits(['change']);
const index=ref(0),hovered=ref(false),focused=ref(false),reduced=ref(false);
const current=computed(()=>props.items[index.value]);
let timer,motion;
function restart(){clearInterval(timer);if(props.items.length>1&&!hovered.value&&!focused.value&&!reduced.value&&!document.hidden)timer=setInterval(()=>go(index.value+1),4500);}
function go(value){index.value=(value+props.items.length)%props.items.length;restart();}
function preference(event){reduced.value=event.matches;}
watch(()=>props.items,items=>{if(index.value>=items.length)index.value=0;restart();});
watch(()=>current.value?.[props.itemKey],()=>emit('change'));
watch([hovered,focused,reduced],restart);
onMounted(()=>{motion=window.matchMedia('(prefers-reduced-motion: reduce)');reduced.value=motion.matches;motion.addEventListener('change',preference);document.addEventListener('visibilitychange',restart);restart();});
onUnmounted(()=>{clearInterval(timer);motion?.removeEventListener('change',preference);document.removeEventListener('visibilitychange',restart);});
</script>
<template>
  <section class="offer-card" :aria-label="label" aria-roledescription="carousel" @mouseenter="hovered=true" @mouseleave="hovered=false" @focusin="focused=!!$event.target.closest('.offer-content')" @focusout="focused=!!$event.relatedTarget?.closest('.offer-content')">
    <span class="offer-eyebrow">{{ label }}</span>
    <div v-if="current" :key="current[itemKey]" class="offer-content"><slot :item="current" /></div>
    <slot v-else name="empty" />
    <div v-if="items.length>1" class="offer-navigation">
      <button :aria-label="`${label}: trước`" @click="go(index-1)">‹</button>
      <span>{{ index+1 }} / {{ items.length }}</span>
      <button :aria-label="`${label}: tiếp theo`" @click="go(index+1)">›</button>
    </div>
    <slot name="footer" />
  </section>
</template>
