import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";

export function connection(input = {}) {
  const url = new URL(input.url || "http://127.0.0.1:18990");
  if (!["http:", "https:"].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    throw new Error("Service URL 必须是无用户名、密码、查询参数的 HTTP(S) 地址");
  }
  if (url.protocol === "http:" && !["localhost", "127.0.0.1", "[::1]"].includes(url.hostname)) {
    throw new Error("远端 Service 必须使用 HTTPS；HTTP 只允许本机回环地址");
  }
  const workspace = input.workspace || "ws_default";
  if (!/^[A-Za-z0-9_-]{1,128}$/.test(workspace)) throw new Error("Workspace ID 格式无效");
  return { url: url.href.replace(/\/$/, ""), workspace };
}

export async function loadConnection(entry, env = process.env) {
  let saved = {};
  try { saved = JSON.parse(await readFile(join(dirname(entry), "connection.json"), "utf8")); }
  catch (error) { if (error.code !== "ENOENT") throw error; }
  return { ...connection({ url: env.WEPUSH_SERVICE_URL || saved.url, workspace: env.WEPUSH_WORKSPACE_ID || saved.workspace }),
    token: env.WEPUSH_API_TOKEN?.trim() || "" };
}
