<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { api, clearCsrf } from "../services/api";
import { useAuth } from "../stores/auth";
import { useTask } from "../lib/shop";
import TaskState from "../components/TaskState.vue";
const profile = ref(null),
  password = ref({ currentPassword: "", password: "" }),
  auth = useAuth(),
  router = useRouter(),
  { busy, error, message, run } = useTask();
onMounted(() => run(async () => (profile.value = await api("/account"))));
function save() {
  run(async () => {
    await api("/account", {
      method: "PUT",
      body: JSON.stringify(profile.value),
    });
    await auth.load();
    message.value = "Đã lưu thông tin.";
  });
}
function change() {
  run(async () => {
    await api("/account/password", {
      method: "POST",
      body: JSON.stringify(password.value),
    });
    auth.user = null;
    clearCsrf();
    router.push("/login");
  });
}
</script>
<template>
  <div class="page-heading">
    <h1>Tài khoản của tôi</h1>
    <p>{{ auth.user?.email }}</p>
  </div>
  <TaskState :busy="busy" :error="error" :message="message" />
  <div class="two-column">
    <form v-if="profile" class="panel form" @submit.prevent="save">
      <h2>Thông tin cá nhân</h2>
      <label
        >Họ tên<input
          v-model="profile.fullName"
          required
          maxlength="100" /></label
      ><label>Điện thoại<input v-model="profile.phone" maxlength="20" /></label
      ><label
        >Địa chỉ<textarea v-model="profile.address" maxlength="500" /></label
      ><button :disabled="busy">Lưu thay đổi</button>
    </form>
    <form class="panel form" @submit.prevent="change">
      <h2>Đổi mật khẩu</h2>
      <label
        >Mật khẩu hiện tại<input
          v-model="password.currentPassword"
          type="password"
          autocomplete="current-password"
          required /></label
      ><label
        >Mật khẩu mới<input
          v-model="password.password"
          type="password"
          minlength="8"
          maxlength="72"
          autocomplete="new-password"
          required
      /></label>
      <p class="muted">Bạn sẽ đăng nhập lại sau khi đổi mật khẩu.</p>
      <button :disabled="busy">Đổi mật khẩu</button>
    </form>
  </div>
</template>
