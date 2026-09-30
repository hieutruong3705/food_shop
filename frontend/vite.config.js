import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";
export default defineConfig({
  plugins: [vue()],
  server: {
    host: "127.0.0.1",
    proxy: {
      "/api": "http://localhost:8080",
      "/images": "http://localhost:8080",
      "/uploads": "http://localhost:8080",
    },
  },
});
