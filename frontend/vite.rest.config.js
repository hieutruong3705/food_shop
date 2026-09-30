import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

// Keep the independently added TypeScript scaffold intact; build the API-backed storefront.
export default defineConfig({
  plugins: [vue(), {
    name: "rest-entry",
    transformIndexHtml: { order: "pre", handler: html => html.replace('/src/main.ts', '/src/main-rest.js').replace('lang="en"', 'lang="vi"') },
  }],
  server: {
    host: "127.0.0.1",
    proxy: Object.fromEntries(["/api", "/images", "/uploads"].map(path => [path, "http://127.0.0.1:8080"])),
  },
});
