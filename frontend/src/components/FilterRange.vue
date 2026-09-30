<script setup>
import { computed, ref, watch } from 'vue';
import RangeSlider from './RangeSlider.vue';

// A range filter: a two-thumb slider plus two number boxes that stay in step. Every change is reported
// as it happens (change: {low, high}); the page decides when to apply it.
// low and high are what is applied now; min and max are what the catalog offers.
const props = defineProps({
  label: { type: String, required: true },
  min: { type: Number, required: true },
  max: { type: Number, required: true },
  low: { type: Number, required: true },
  high: { type: Number, required: true },
  step: { type: Number, default: 1 },
  format: { type: Function, default: (value) => String(value) },
});
const emit = defineEmits(['change']);

const lo = ref(props.low), hi = ref(props.high);
watch(() => [props.low, props.high], ([low, high]) => { lo.value = low; hi.value = high; });
// A step that does not divide the span would make the top of the range unreachable.
const slideStep = computed(() => ((props.max - props.min) % props.step === 0 ? props.step : 1));
const clamp = (value) => Math.min(props.max, Math.max(props.min, Number.isFinite(value) ? value : props.min));

const commit = () => emit('change', { low: lo.value, high: hi.value });
function typed(which, event) {
  const value = clamp(Math.round(Number(event.target.value)));
  if (which === 'low') lo.value = Math.min(value, hi.value);
  else hi.value = Math.max(value, lo.value);
  event.target.value = which === 'low' ? lo.value : hi.value;
  commit();
}
</script>

<template>
  <div class="filter-range">
    <RangeSlider :min="min" :max="max" v-model:low="lo" v-model:high="hi" :step="slideStep" :label="label" :format="format" @update:low="commit" @update:high="commit" />
    <div v-if="max > min" class="range-inputs">
      <input type="number" :aria-label="`${label} từ (nhập số)`" :min="min" :max="max" :step="step" :value="lo" @change="typed('low', $event)" />
      <span aria-hidden="true">–</span>
      <input type="number" :aria-label="`${label} đến (nhập số)`" :min="min" :max="max" :step="step" :value="hi" @change="typed('high', $event)" />
    </div>
  </div>
</template>
