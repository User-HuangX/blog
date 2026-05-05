import path from "node:path";
import {fileURLToPath} from "node:url";
import tailwindcss from "@tailwindcss/vite";
import {defineConfig} from "vite";
import vue from "@vitejs/plugin-vue";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const localApiTarget = process.env.VITE_PROXY_TARGET || "http://localhost:19080";

export default defineConfig({
    plugins: [vue(), tailwindcss()],
    root: "src-frontend",
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "src-frontend"),
        },
    },
    server: {
        host: true,
        port: 5173,
        proxy: {
            "/api": {
                target: localApiTarget,
                changeOrigin: true,
            },
            "/uploads": {
                target: localApiTarget,
                changeOrigin: true,
            },
        },
    },
});
