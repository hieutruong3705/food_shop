<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { XMarkIcon } from '@heroicons/vue/24/outline';

const props = defineProps<{
  message: string;
  type?: 'success' | 'error' | 'info';
  duration?: number;
}>();

const emit = defineEmits(['close']);
const visible = ref(true);

onMounted(() => {
  if (props.duration !== 0) {
    setTimeout(() => {
      close();
    }, props.duration || 3000);
  }
});

function close() {
  visible.value = false;
  emit('close');
}
</script>

<template>
  <transition
    enter-active-class="transform ease-out duration-300 transition"
    enter-from-class="translate-y-2 opacity-0 sm:translate-y-0 sm:translate-x-2"
    enter-to-class="translate-y-0 opacity-100 sm:translate-x-0"
    leave-active-class="transition ease-in duration-100"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div v-if="visible" class="max-w-sm w-full bg-white shadow-lg rounded-lg pointer-events-auto ring-1 ring-black ring-opacity-5 overflow-hidden">
      <div class="p-4">
        <div class="flex items-start">
          <div class="flex-shrink-0">
            <!-- Icon based on type -->
            <div v-if="type === 'success'" class="h-6 w-6 text-green-400 rounded-full bg-green-100 flex items-center justify-center">✓</div>
            <div v-else-if="type === 'error'" class="h-6 w-6 text-red-400 rounded-full bg-red-100 flex items-center justify-center">!</div>
            <div v-else class="h-6 w-6 text-blue-400 rounded-full bg-blue-100 flex items-center justify-center">i</div>
          </div>
          <div class="ml-3 w-0 flex-1 pt-0.5">
            <p class="text-sm font-medium text-gray-900">
              {{ message }}
            </p>
          </div>
          <div class="ml-4 flex-shrink-0 flex">
            <button @click="close" class="bg-white rounded-md inline-flex text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary">
              <span class="sr-only">Close</span>
              <XMarkIcon class="h-5 w-5" aria-hidden="true" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>
