<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  currentPage: number;
  totalPages: number;
}>();

const emit = defineEmits<{
  (e: 'page-change', page: number): void
}>();

const pages = computed(() => {
  const range = [];
  for (let i = 0; i < props.totalPages; i++) {
    range.push(i);
  }
  return range;
});
</script>

<template>
  <div v-if="totalPages > 1" class="flex items-center justify-center space-x-2 my-8">
    <button 
      @click="emit('page-change', currentPage - 1)" 
      :disabled="currentPage === 0"
      class="px-3 py-1 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
    >
      Previous
    </button>
    
    <button 
      v-for="page in pages" 
      :key="page"
      @click="emit('page-change', page)"
      class="px-3 py-1 border rounded-md"
      :class="page === currentPage ? 'bg-primary text-white border-primary' : 'hover:bg-gray-50'"
    >
      {{ page + 1 }}
    </button>
    
    <button 
      @click="emit('page-change', currentPage + 1)" 
      :disabled="currentPage === totalPages - 1"
      class="px-3 py-1 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
    >
      Next
    </button>
  </div>
</template>
