import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import { execFile } from "node:child_process";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const serviceTarget = process.env.WEPUSH_SERVICE_URL ?? "http://127.0.0.1:18990";
const aiRoot = fileURLToPath(new URL("../../packages/ai-integration/", import.meta.url));
const aiBundle = new URL("../../packages/ai-integration/dist/wepush-ai.mjs", import.meta.url);

export default defineConfig({
  plugins: [react(), tailwindcss(), {
    name: "wepush-ai-installer",
    async buildStart() { await promisify(execFile)(process.execPath, ["build.mjs"], { cwd: aiRoot }); },
    async generateBundle() { this.emitFile({ type: "asset", fileName: "ai/wepush-ai.mjs", source: await readFile(aiBundle) }); },
    configureServer(server) {
      server.middlewares.use("/ai/wepush-ai.mjs", async (_request, response, next) => {
        try { response.setHeader("Content-Type", "text/javascript; charset=utf-8"); response.end(await readFile(aiBundle)); }
        catch (error) { next(error); }
      });
    },
  }],
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
