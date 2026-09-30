<script setup>
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth } from "../stores/auth";
import { api, clearCsrf } from "../services/api";
import { useTask } from "../lib/shop";
import TaskState from "../components/TaskState.vue";
const route = useRoute(),
  router = useRouter(),
  auth = useAuth(),
  form = reactive({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
const { busy, error, message, run } = useTask();
const resendEmail = ref("");
const mode = computed(() => route.path.slice(1)),
  titles = {
    login: "Chào mừng trở lại",
    register: "Tạo tài khoản mới",
    "forgot-password": "Quên mật khẩu",
    "reset-password": "Đặt lại mật khẩu",
    verify: "Xác minh email",
  };
watch(
  mode,
  () => {
    error.value = "";
    message.value = "";
    resendEmail.value = "";
    form.password = "";
    form.confirmPassword = "";
    if (mode.value === "verify")
      run(async () => {
        const r = await api(
          "/auth/verify", { method: "POST", body: JSON.stringify({ code: route.query.code || "" }) },
        );
        if (!r.verified)
          throw new Error("Liên kết không hợp lệ hoặc đã được sử dụng.");
        message.value = "Đã xác minh email. Bạn có thể đăng nhập.";
      });
  },
  { immediate: true },
);
function resend(email) {
  run(async () => {
    const r = await api("/auth/resend-verification", {
      method: "POST",
      body: JSON.stringify({ email }),
    });
    message.value = r.message;
  });
}
function submit() {
  run(async () => {
    if (mode.value === "login") {
      await auth.login(form);
      const target = String(route.query.redirect || "/");
      await router.push(
        target.startsWith("/") && !target.startsWith("//") ? target : "/",
      );
    } else {
      if (
        ["register", "reset-password"].includes(mode.value) &&
        form.password !== form.confirmPassword
      )
        throw new Error("Mật khẩu xác nhận không khớp");
      const r = await api(`/auth/${mode.value}`, {
        method: "POST",
        body: JSON.stringify({ ...form, token: route.query.token }),
      });
      clearCsrf();
      message.value = r.message;
      form.password = "";
      form.confirmPassword = "";
    }
  });
}
</script>
<template>
  <section class="auth-layout">
    <div class="auth-aside">
      <span class="eyebrow">TECHSHOP MEMBERS</span>
      <h1>Công nghệ mới.<br />Trải nghiệm mới.</h1>
      <img src="/tech/headphones.svg" alt="Tai nghe công nghệ" />
      <p>Theo dõi đơn hàng, lưu giỏ hàng và khám phá thiết bị dành cho bạn.</p>
    </div>
    <div class="auth-form">
      <span class="eyebrow">TÀI KHOẢN TECHSHOP</span>
      <h2>{{ titles[mode] }}</h2>
      <TaskState :busy="busy" :error="error" :message="message" />
      <form v-if="mode !== 'verify'" class="form" @submit.prevent="submit">
        <template v-if="mode === 'register'"
          ><label
            >Họ<input
              v-model="form.firstName"
              autocomplete="family-name"
              required
              maxlength="50" /></label
          ><label
            >Tên<input
              v-model="form.lastName"
              autocomplete="given-name"
              required
              maxlength="50" /></label></template
        ><label v-if="mode !== 'reset-password'"
          >Email<input
            v-model="form.email"
            type="email"
            autocomplete="email"
            required /></label
        ><label v-if="mode !== 'forgot-password'"
          >Mật khẩu<input
            v-model="form.password"
            type="password"
            :autocomplete="
              mode === 'login' ? 'current-password' : 'new-password'
            "
            :minlength="mode === 'login' ? 1 : 8"
            maxlength="72"
            required /></label
        ><label v-if="['register', 'reset-password'].includes(mode)"
          >Xác nhận mật khẩu<input
            v-model="form.confirmPassword"
            type="password"
            autocomplete="new-password"
            required /></label
        ><button :disabled="busy">
          {{
            mode === "login"
              ? "Đăng nhập"
              : mode === "register"
                ? "Đăng ký"
                : "Gửi yêu cầu"
          }}
          →
        </button>
      </form>
      <button
        v-if="(mode === 'register' && message) || (mode === 'login' && error && form.email)"
        type="button"
        class="secondary"
        :disabled="busy"
        @click="resend(form.email)"
      >
        Gửi lại email xác minh
      </button>
      <form
        v-if="mode === 'verify' && error"
        class="form"
        @submit.prevent="resend(resendEmail)"
      >
        <label
          >Email đã đăng ký<input
            v-model="resendEmail"
            type="email"
            autocomplete="email"
            maxlength="254"
            required /></label
        ><button :disabled="busy">Gửi lại email xác minh</button>
      </form>
      <div class="auth-links">
        <RouterLink v-if="mode !== 'login'" to="/login">Đăng nhập</RouterLink
        ><RouterLink v-if="mode === 'login'" to="/forgot-password"
          >Quên mật khẩu?</RouterLink
        ><RouterLink v-if="mode !== 'register'" to="/register"
          >Tạo tài khoản</RouterLink
        >
      </div>
    </div>
  </section>
</template>
