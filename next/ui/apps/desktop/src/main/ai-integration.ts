import { app, dialog } from "electron";
import { execFile } from "node:child_process";
import { writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const execute = promisify(execFile);

export async function installAiIntegration(target: string, url: string, workspace: string) {
  if (!["codex", "skill", "generic"].includes(target) || typeof url !== "string" || typeof workspace !== "string") {
    throw new Error("无效的 AI 接入参数");
  }
  const entry = app.isPackaged ? join(process.resourcesPath, "ai", "wepush-ai.mjs")
    : resolve(dirname(fileURLToPath(import.meta.url)), "../../../../packages/ai-integration/dist/wepush-ai.mjs");
  try {
    const result = await execute(process.execPath, [entry, "install", "--target", target, "--url", url, "--workspace", workspace], {
      env: { ...process.env, ELECTRON_RUN_AS_NODE: "1" }, timeout: 30_000, maxBuffer: 1024 * 1024,
    });
    const installed = JSON.parse(result.stdout) as { message: string; destination: string; configPath?: string; backup?: string; mcpConfig: unknown };
    const paths = [installed.destination, installed.configPath, installed.backup ? `备份：${installed.backup}` : undefined].filter(Boolean);
    if (target === "generic") {
      const selected = await dialog.showSaveDialog({ defaultPath: "wepush-mcp.json", filters: [{ name: "MCP JSON", extensions: ["json"] }] });
      if (!selected.canceled && selected.filePath) {
        await writeFile(selected.filePath, JSON.stringify(installed.mcpConfig, null, 2) + "\n", { mode: 0o600 });
        paths.push(selected.filePath);
      }
      return { ok: true, message: "通用 MCP 配置已生成，可合并到支持 stdio 的客户端。", output: `${paths.join("\n")}\n${JSON.stringify(installed.mcpConfig, null, 2)}` };
    }
    return { ok: true, message: installed.message, output: paths.join("\n") };
  } catch (error) {
    const problem = error as Error & { stderr?: string };
    return { ok: false, message: problem.stderr?.trim() || problem.message, output: "" };
  }
}
