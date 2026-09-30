<script setup>
import { computed } from 'vue';
/** Dual-thumb slider; bounds come from the data so the track scales to the real range. */
const props = defineProps({ min:{type:Number,required:true}, max:{type:Number,required:true}, low:{type:Number,required:true}, high:{type:Number,required:true}, step:{type:Number,default:1}, label:{type:String,required:true}, unit:{type:String,default:''} });
const emit = defineEmits(['update:low','update:high','change']);
const span = computed(() => Math.max(props.max - props.min, 1));
const left = computed(() => (props.low - props.min) / span.value * 100);
const right = computed(() => (props.high - props.min) / span.value * 100);
function setLow(value) { emit('update:low', Math.min(Number(value), props.high)); }
function setHigh(value) { emit('update:high', Math.max(Number(value), props.low)); }
</script>
<template>
  <p v-if="max <= min" class="muted range-fixed">Tất cả sản phẩm: {{ min }} {{ unit }}</p>
  <div v-else class="range-slider">
    <div class="range-values"><span>{{ low }} {{ unit }}</span><span>{{ high }} {{ unit }}</span></div>
    <div class="range-track"><div class="range-fill" :style="{ left: `calc(9px + (100% - 18px) * ${left / 100})`, width: `calc((100% - 18px) * ${(right - left) / 100})` }"></div>
      <input type="range" :aria-label="`${label} từ`" :min="min" :max="max" :step="step" :value="low" @input="setLow($event.target.value)" @change="emit('change')" />
      <input type="range" :aria-label="`${label} đến`" :min="min" :max="max" :step="step" :value="high" @input="setHigh($event.target.value)" @change="emit('change')" />
    </div>
    <div class="range-bounds"><small>{{ min }}</small><small>{{ max }}</small></div>
  </div>
</template>
