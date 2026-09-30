<script setup>
defineProps({ busy: Boolean, error: String, message: String });
</script>
<template>
  <p v-if="busy" class="notice" role="status">Đang xử lý…</p>
  <p v-if="error" class="notice error" role="alert">{{ error }}</p>
  <p v-if="message" class="notice success" role="status">{{ message }}</p>
</template>
