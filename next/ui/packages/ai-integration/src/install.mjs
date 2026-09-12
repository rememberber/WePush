import { copyFile, lstat, mkdir, mkdtemp, readFile, rename, rm, writeFile } from "node:fs/promises";
import { homedir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { randomUUID } from "node:crypto";
import { isDeepStrictEqual } from "node:util";
import { parse, stringify } from "smol-toml";
import { connection } from "./config.mjs";

const begin = "# BEGIN WEPUSH MANAGED MCP";
const end = "# END WEPUSH MANAGED MCP";
const owner = "wepush-next-ai-v1";
const sh = (value) => `'${value.replaceAll("'", "'\\''")}'`;
const ps = (value) => `'${value.replaceAll("'", "''")}'`;

async function readOptional(path) {
  try {
    if (!(await lstat(path)).isFile()) throw new Error(`拒绝覆盖非普通文件：${path}`);
    return await readFile(path, "utf8");
  } catch (error) { if (error.code === "ENOENT") return ""; throw error; }
}

export function codexConfig(current, server) {
  const original = parse(current);
  const block = `${begin}\n${stringify({ mcp_servers: { wepush: { ...server, env_vars: ["WEPUSH_API_TOKEN"] } } })}${end}`;
  const managed = /^# BEGIN WEPUSH MANAGED MCP\r?\n[\s\S]*?^# END WEPUSH MANAGED MCP\r?$/gm;
  const matches = [...current.matchAll(managed)];
  if (matches.length > 1 || (current.includes(begin) && matches.length !== 1)) {
    throw new Error("Codex 的 WePush 配置块损坏，请先检查 config.toml");
  }
  if (original.mcp_servers?.wepush && matches.length === 0) {
    throw new Error("Codex 已存在手动配置的 wepush MCP；请先重命名或移除该条目，安装器不会覆盖它。");
  }
  const next = matches.length ? current.replace(managed, () => block) : `${current}${current.endsWith("\n") || !current ? "" : "\n"}\n${block}\n`;
  const parsed = parse(next);
  const withoutWePush = (value) => {
    if (value.mcp_servers) {
      delete value.mcp_servers.wepush;
      if (Object.keys(value.mcp_servers).length === 0) delete value.mcp_servers;
    }
    return value;
  };
  if (!isDeepStrictEqual(withoutWePush(original), withoutWePush(parsed))) {
    throw new Error("配置块中包含其他 Codex 设置；拒绝覆盖，请手动整理配置。");
  }
  return next;
}

export async function install({ entry, skill, target = "codex", url, workspace,
  home = homedir(), codexHome = process.env.CODEX_HOME || join(home, ".codex"),
  runtime = process.execPath, electron = Boolean(process.versions.electron) }) {
  if (!["codex", "skill", "generic"].includes(target)) throw new Error("安装目标必须是 codex、skill 或 generic");
  const config = connection({ url, workspace });
  const destination = resolve(home, target === "generic" ? ".wepush/ai" : ".agents/skills/wepush");
  await mkdir(dirname(destination), { recursive: true });
  const lock = `${destination}.install-lock`;
  try { await mkdir(lock); }
  catch (error) { if (error.code === "EEXIST") throw new Error(`另一次安装尚未完成：${lock}`); throw error; }
  let stage;
  let previous;
  let installed = false;
  try {
    let exists = false;
    try {
      const stat = await lstat(destination);
      if (!stat.isDirectory() || stat.isSymbolicLink()) throw new Error(`安装目标不是普通目录：${destination}`);
      exists = true;
    } catch (error) { if (error.code !== "ENOENT") throw error; }
    if (exists && (await readOptional(join(destination, ".wepush-install"))).trim() !== owner) {
      throw new Error(`已存在非 WePush 安装器管理的 Skill；不会覆盖：${destination}`);
    }
    const script = join(destination, "scripts", "wepush-ai.mjs");
    const server = { command: runtime, args: [script, "mcp"],
      env: { WEPUSH_SERVICE_URL: config.url, WEPUSH_WORKSPACE_ID: config.workspace,
        ...(electron ? { ELECTRON_RUN_AS_NODE: "1" } : {}) } };
    const configPath = join(codexHome, "config.toml");
    const original = target === "codex" ? await readOptional(configPath) : "";
    const updated = target === "codex" ? codexConfig(original, server) : "";
    stage = await mkdtemp(`${destination}.install-`);
    await mkdir(join(stage, "scripts"));
    await copyFile(entry, join(stage, "scripts", "wepush-ai.mjs"));
    await writeFile(join(stage, "scripts", "connection.json"), JSON.stringify(config, null, 2) + "\n", { mode: 0o600 });
    await writeFile(join(stage, "SKILL.md"), skill);
    await writeFile(join(stage, ".wepush-install"), owner + "\n");
    await writeFile(join(stage, "scripts", "wepush-ai.sh"),
      `#!/bin/sh\n${electron ? "export ELECTRON_RUN_AS_NODE=1\n" : ""}exec ${sh(runtime)} ${sh(script)} "$@"\n`, { mode: 0o755 });
    await writeFile(join(stage, "scripts", "wepush-ai.ps1"),
      `\uFEFF${electron ? "$env:ELECTRON_RUN_AS_NODE = '1'\n" : ""}& ${ps(runtime)} ${ps(script)} @args\nexit $LASTEXITCODE\n`);
    const mcpConfig = { mcpServers: { wepush: server } };
    await writeFile(join(stage, "mcp.json"), JSON.stringify(mcpConfig, null, 2) + "\n");
    if (exists) {
      previous = `${destination}.previous-${randomUUID()}`;
      await rename(destination, previous);
    }
    await rename(stage, destination);
    stage = undefined;
    installed = true;
    let backup;
    if (target === "codex" && original !== updated) {
      await mkdir(codexHome, { recursive: true, mode: 0o700 });
      if (await readOptional(configPath) !== original) throw new Error("Codex 配置在安装期间发生变化，请重试");
      if (original) {
        backup = `${configPath}.wepush-${randomUUID()}.bak`;
        await writeFile(backup, original, { flag: "wx", mode: 0o600 });
      }
      const pending = `${configPath}.wepush-${randomUUID()}.tmp`;
      try {
        await writeFile(pending, updated, { flag: "wx", mode: 0o600 });
        await rename(pending, configPath);
      } finally { await rm(pending, { force: true }); }
    }
    // Installation is committed. Cleanup failure must not roll back the new MCP configuration.
    installed = false;
    if (previous) await rm(previous, { recursive: true, force: true }).catch(() => {});
    previous = undefined;
    return { ok: true, destination, configPath: target === "codex" ? configPath : undefined,
      backup, mcpConfig, message: target === "codex" ? "已安装 Codex MCP 和 WePush Skill；请重新连接 MCP 或重启 Codex。"
        : target === "skill" ? "已安装 WePush Skill；可通过 $wepush 使用。" : "已准备通用 MCP 配置和 Skill。" };
  } catch (error) {
    if (installed) await rm(destination, { recursive: true, force: true });
    if (previous) await rename(previous, destination);
    throw error;
  } finally {
    if (stage) await rm(stage, { recursive: true, force: true });
    await rm(lock, { recursive: true, force: true });
  }
}
