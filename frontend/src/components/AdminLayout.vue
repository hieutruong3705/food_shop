<script setup>
import {ref,watch} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import {useAuth} from '../stores/auth';
import {resources} from '../lib/admin';
const auth=useAuth(),route=useRoute(),router=useRouter(),error=ref(''),loggingOut=ref(false);
watch(()=>route.fullPath,()=>error.value='');
async function logout(){loggingOut.value=true;try{await auth.logout();router.push('/login');}catch(e){error.value=e.message;}finally{loggingOut.value=false;}}
</script>
<template>
  <div class="admin-shell">
    <aside class="admin-sidebar"><RouterLink class="admin-logo" to="/admin">TechShop <small>QUẢN TRỊ</small></RouterLink><nav aria-label="Điều hướng quản trị"><RouterLink to="/admin">Tổng quan</RouterLink><RouterLink v-for="(resource,key) in resources" :key="key" :to="`/admin/${key}`">{{ resource.title }}</RouterLink></nav></aside>
    <div class="admin-workspace"><header class="admin-topbar"><strong>{{ resources[route.params.resource]?.title || 'Tổng quan' }}</strong><div><span>{{ auth.user?.fullName || auth.user?.email }}</span><RouterLink to="/">Xem cửa hàng ↗</RouterLink><button class="secondary" :disabled="loggingOut" @click="logout">Đăng xuất</button></div></header><main class="admin-main"><p v-if="error" class="notice error" role="alert">{{ error }}</p><slot /></main></div>
  </div>
</template>
