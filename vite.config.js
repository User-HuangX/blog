import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const localApiTarget = process.env.VITE_PROXY_TARGET || "http://localhost:19080";

export default defineConfig({
    plugins: [vue()],
    root: "src-frontend",
    server: {
        host: true,
        port: 5173,
        proxy: {
            "/api": {
                target: localApiTarget,
                changeOrigin: true
            },
            "/uploads": {
                target: localApiTarget,
                changeOrigin: true
            }
        }
    }
});
