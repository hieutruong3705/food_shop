<script setup>
import { ref, onMounted } from "vue";
import { api } from "../services/api";
import { useTask } from "../lib/shop";
import TaskState from "../components/TaskState.vue";
const items = ref([]),
  { busy, error, run } = useTask();
function plain(value) {
  const doc = new DOMParser().parseFromString(value || "", "text/html");
  return doc.body.textContent || "";
}
onMounted(() => run(async () => (items.value = await api("/policies"))));
</script>
<template>
  <div class="page-heading">
    <h1>Chính sách TechShop</h1>
    <p>Thông tin mua hàng, bảo hành và quyền riêng tư.</p>
  </div>
  <TaskState :busy="busy" :error="error" />
  <article v-for="p in items" :key="p.id" class="panel section">
    <h2>{{ p.name }}</h2>
    <p class="prewrap">{{ plain(p.content) }}</p>
  </article>
  <p v-if="!busy && !error && !items.length" class="empty">
    Chính sách đang được cập nhật.
  </p>
</template>
