import { defineStore } from "pinia";
import { api, clearCsrf } from "../services/api";
export const useAuth = defineStore("auth", {
  state: () => ({ user: null, loaded: false }),
  getters: { admin: (state) => state.user?.role === "ADMIN" },
  actions: {
    async load() {
      try {
        this.user = await api("/auth/me");
      } catch (e) {
        if (e.status !== 401) throw e;
        this.user = null;
      }
      this.loaded = true;
    },
    async login(input) {
      this.user = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify(input),
      });
      clearCsrf();
      this.loaded = true;
    },
    async logout() {
      await api("/auth/logout", { method: "POST" });
      clearCsrf();
      this.user = null;
      this.loaded = true;
    },
  },
});
