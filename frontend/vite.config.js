import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [vue()],
  test: { environment: "jsdom", include: ["tests-unit/**/*.spec.js"] },
  server: {
    host: "127.0.0.1",
    proxy: Object.fromEntries(["/api", "/images", "/uploads", "/oauth2", "/login/oauth2"].map(path => [path, "http://127.0.0.1:8080"])),
  },
});
