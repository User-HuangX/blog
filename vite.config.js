import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
    plugins: [vue()],
    root: "src-frontend",
    server: {
        host: true,
        port: 5173,
        proxy: {
            "/api": {
                target: process.env.VITE_PROXY_TARGET || "http://localhost:8080",
                changeOrigin: true
            },
            "/uploads": {
                target: process.env.VITE_PROXY_TARGET || "http://localhost:8080",
                changeOrigin: true
            }
        }
    }
});
