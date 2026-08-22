import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const serviceTarget = process.env.WEPUSH_SERVICE_URL ?? "http://127.0.0.1:18990";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": serviceTarget,
      "/actuator": serviceTarget,
      "/openapi.yaml": serviceTarget,
    },
  },
  build: {
    target: "es2022",
    sourcemap: true,
  },
});
