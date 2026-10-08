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
const googleEnabled = ref(false);
const googleErrors = {
  google: "Không đăng nhập được bằng Google. Vui lòng thử lại.",
  "google-email": "Tài khoản Google chưa xác minh email nên không thể dùng để đăng nhập.",
  "google-locked": "Tài khoản đã bị khóa.",
};
api("/auth/providers")
  .then((r) => (googleEnabled.value = !!r.google))
  .catch(() => (googleEnabled.value = false));
/* The browser leaves the OAuth chain with a full page load, so the outcome arrives as a query string. */
if (googleErrors[route.query.error]) error.value = googleErrors[route.query.error];
function signInWithGoogle() {
  window.location.href = "/oauth2/authorization/google";
}
const mode = computed(() => route.path.slice(1)),
  titles = {
    login: "Chào mừng trở lại",
    register: "Tạo tài khoản mới",
    "forgot-password": "Quên mật khẩu",
    "reset-password": "Đặt lại mật khẩu",
    verify: "Xác minh email",
  };
let firstRun = true;
watch(
  mode,
  () => {
    /* Keep the error the OAuth redirect just put there; clear it on every later navigation. */
    if (!firstRun) error.value = "";
    firstRun = false;
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
      <template v-if="googleEnabled && ['login', 'register'].includes(mode)">
        <div class="auth-divider">hoặc</div>
        <button type="button" class="oauth-button" @click="signInWithGoogle">
          <svg viewBox="0 0 18 18" aria-hidden="true">
            <path fill="#4285F4" d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.92c1.7-1.57 2.68-3.88 2.68-6.62Z" />
            <path fill="#34a853" d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.92-2.26c-.8.54-1.84.86-3.04.86-2.34 0-4.33-1.58-5.04-3.71H.96v2.33A9 9 0 0 0 9 18Z" />
            <path fill="#FBBC05" d="M3.96 10.71a5.41 5.41 0 0 1 0-3.42V4.96H.96a9 9 0 0 0 0 8.08l3-2.33Z" />
            <path fill="#EA4335" d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58C13.46.89 11.43 0 9 0A9 9 0 0 0 .96 4.96l3 2.33C4.67 5.16 6.66 3.58 9 3.58Z" />
          </svg>
          Tiếp tục với Google
        </button>
      </template>
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
