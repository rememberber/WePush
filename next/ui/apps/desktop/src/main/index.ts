import { app, BrowserWindow, dialog, ipcMain, net, protocol, safeStorage, session, shell } from "electron";
import { execFile } from "node:child_process";
import { randomUUID } from "node:crypto";
import { access, chmod, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve, sep } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { promisify } from "node:util";

const currentDirectory = dirname(fileURLToPath(import.meta.url));
const developmentUrl = process.env.WEPUSH_UI_URL ?? "http://127.0.0.1:5173";
const productionOrigin = "wepush://app";
const rendererRoot = app.isPackaged
  ? join(process.resourcesPath, "web/dist")
  : resolve(currentDirectory, "../../../web/dist");
const executeFile = promisify(execFile);
const serviceName = "WePushNextService";
const serviceLabel = "com.fangxuele.wepush-next.service";

interface CommandResult {
  ok: boolean;
  message: string;
  output: string;
}

interface ServiceStatus {
  installed: boolean;
  running: boolean;
  platform: NodeJS.Platform;
  detail: string;
}

protocol.registerSchemesAsPrivileged([{
  scheme: "wepush",
  privileges: {
    standard: true,
    secure: true,
    supportFetchAPI: true,
    corsEnabled: true,
  },
}]);

async function createWindow(): Promise<void> {
  const window = new BrowserWindow({
    width: 1360,
    height: 860,
    minWidth: 920,
    minHeight: 640,
    title: "WePush Next",
    backgroundColor: "#f7f7f5",
    show: false,
    titleBarStyle: process.platform === "darwin" ? "hiddenInset" : "default",
    webPreferences: {
      preload: join(currentDirectory, "../preload/index.cjs"),
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      webSecurity: true,
    },
  });

  window.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith("https://") || url.startsWith("http://127.0.0.1:18990/")) {
      void shell.openExternal(url);
    }
    return { action: "deny" };
  });
  window.webContents.on("will-navigate", (event, url) => {
    let sameOrigin = false;
    try {
      const expected = new URL(app.isPackaged ? productionOrigin : developmentUrl);
      const target = new URL(url);
      sameOrigin = target.protocol === expected.protocol && target.host === expected.host;
    } catch {
      // Malformed navigation targets are denied below.
    }
    if (!sameOrigin) event.preventDefault();
  });
  window.once("ready-to-show", () => window.show());

  if (app.isPackaged) {
    await window.loadURL(`${productionOrigin}/index.html`);
  } else {
    await window.loadURL(developmentUrl);
  }
  if (process.env.WEPUSH_DESKTOP_SMOKE_TEST === "true") {
    const rendered = await window.webContents.executeJavaScript(
      "Boolean(document.querySelector('#root')?.textContent?.includes('WePush'))",
    ) as boolean;
    if (!rendered) throw new Error("Desktop renderer smoke check failed");
    console.log("WePush Next Desktop smoke check passed");
    app.quit();
  }
}

function senderIsTrusted(url: string): boolean {
  try {
    const actual = new URL(url);
    const expected = new URL(app.isPackaged ? productionOrigin : developmentUrl);
    return actual.protocol === expected.protocol && actual.host === expected.host;
  } catch {
    return false;
  }
}

function registerIpcHandlers(): void {
  const trusted = <T extends unknown[], R>(handler: (...args: T) => Promise<R> | R) =>
    async (event: Electron.IpcMainInvokeEvent, ...args: T): Promise<R> => {
      if (!event.senderFrame || !senderIsTrusted(event.senderFrame.url)) throw new Error("Untrusted Desktop IPC caller");
      return handler(...args);
    };

  ipcMain.handle("wepush:token:load", trusted(async () => loadToken()));
  ipcMain.handle("wepush:token:save", trusted(async (token: string) => saveToken(token)));
  ipcMain.handle("wepush:token:clear", trusted(async () => clearToken()));
  ipcMain.handle("wepush:service:status", trusted(async () => localServiceStatus()));
  ipcMain.handle("wepush:service:start", trusted(async () => controlService("start")));
  ipcMain.handle("wepush:service:stop", trusted(async () => controlService("stop")));
  ipcMain.handle("wepush:service:logs", trusted(async () => serviceLogs()));
  ipcMain.handle("wepush:service:diagnose", trusted(async () => diagnoseInstallation()));
  ipcMain.handle("wepush:plugin:select-stage", trusted(async () => selectAndStagePlugin()));
  ipcMain.handle("wepush:plugin:activate", trusted(async (name: string) => pluginAction("activate", name)));
  ipcMain.handle("wepush:plugin:rollback", trusted(async (name: string) => pluginAction("rollback", name)));
}

function tokenPath(): string {
  return join(app.getPath("userData"), "api-token.enc");
}

function secureStorageAvailable(): boolean {
  if (!safeStorage.isEncryptionAvailable()) return false;
  return process.platform !== "linux" || safeStorage.getSelectedStorageBackend() !== "basic_text";
}

async function loadToken(): Promise<string> {
  try {
    if (!secureStorageAvailable()) return "";
    return safeStorage.decryptString(Buffer.from(await readFile(tokenPath(), "utf8"), "base64"));
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === "ENOENT") return "";
    throw error;
  }
}

async function saveToken(token: string): Promise<void> {
  const normalized = token.trim();
  if (!normalized) return clearToken();
  if (!secureStorageAvailable()) {
    throw new Error("系统安全存储不可用；为避免明文落盘，Desktop 拒绝保存 API Token");
  }
  await mkdir(dirname(tokenPath()), { recursive: true, mode: 0o700 });
  await writeFile(tokenPath(), safeStorage.encryptString(normalized).toString("base64"), { mode: 0o600 });
  if (process.platform !== "win32") await chmod(tokenPath(), 0o600);
}

async function clearToken(): Promise<void> {
  await rm(tokenPath(), { force: true });
}

async function run(command: string, args: string[], timeout = 30_000): Promise<CommandResult> {
  try {
    const result = await executeFile(command, args, { timeout, maxBuffer: 1024 * 1024 });
    const output = `${result.stdout ?? ""}${result.stderr ?? ""}`.trim();
    return { ok: true, message: output || "操作完成", output };
  } catch (error) {
    const problem = error as Error & { stdout?: string; stderr?: string };
    const output = `${problem.stdout ?? ""}${problem.stderr ?? ""}`.trim();
    return { ok: false, message: output || problem.message, output };
  }
}

async function localServiceStatus(): Promise<ServiceStatus> {
  if (process.platform === "win32") {
    const result = await run("powershell.exe", ["-NoProfile", "-NonInteractive", "-Command",
      `$s=Get-Service -Name '${serviceName}' -ErrorAction SilentlyContinue;if($s){$s.Status.ToString()}else{'MISSING'}`]);
    const state = result.output.trim();
    return { installed: state !== "MISSING" && result.ok, running: state === "Running", platform: process.platform, detail: state };
  }
  if (process.platform === "darwin") {
    const result = await run("launchctl", ["print", `system/${serviceLabel}`]);
    const plist = "/Library/LaunchDaemons/com.fangxuele.wepush-next.service.plist";
    return { installed: result.ok || await exists(plist), running: result.ok && /state = running/.test(result.output), platform: process.platform, detail: result.ok ? firstLines(result.output, 12) : result.message };
  }
  const result = await run("systemctl", ["show", "wepush-next-service.service", "--property=LoadState,ActiveState,SubState", "--no-pager"]);
  return { installed: result.ok && /LoadState=loaded/.test(result.output), running: /ActiveState=active/.test(result.output), platform: process.platform, detail: result.output || result.message };
}

async function controlService(action: "start" | "stop"): Promise<CommandResult> {
  if (process.platform === "win32") {
    return elevatedPowerShell(`${action === "start" ? "Start" : "Stop"}-Service -Name '${serviceName}' -ErrorAction Stop`);
  }
  if (process.platform === "darwin") {
    const command = action === "start"
      ? `launchctl enable system/${serviceLabel}; launchctl kickstart -k system/${serviceLabel}`
      : `launchctl disable system/${serviceLabel}; launchctl kill TERM system/${serviceLabel}`;
    return run("osascript", ["-e", `do shell script "${appleScriptString(command)}" with administrator privileges`], 60_000);
  }
  const pkexec = await executable("pkexec");
  if (!pkexec) return { ok: false, message: "未找到 pkexec；请在终端用 sudo systemctl 管理 WePush Next Service", output: "" };
  return run(pkexec, ["systemctl", action, "wepush-next-service.service"], 60_000);
}

async function serviceLogs(): Promise<CommandResult> {
  if (process.platform === "win32") {
    const root = join(process.env.ProgramData ?? "C:\\ProgramData", "WePush Next", "logs");
    return run("powershell.exe", ["-NoProfile", "-NonInteractive", "-Command",
      `Get-ChildItem -LiteralPath '${powerShellString(root)}' -Filter 'WePushNextService*' -File -ErrorAction SilentlyContinue|Sort-Object LastWriteTime|Select-Object -Last 3|ForEach-Object {\"=== $($_.Name) ===\";Get-Content -LiteralPath $_.FullName -Tail 120}`]);
  }
  if (process.platform === "darwin") {
    return tailFiles(["/Library/Logs/WePushNext/service.log", "/Library/Logs/WePushNext/service-error.log"]);
  }
  return run("journalctl", ["--unit=wepush-next-service.service", "--lines=200", "--no-pager"]);
}

async function diagnoseInstallation(): Promise<CommandResult> {
  const status = await localServiceStatus();
  const root = releaseRoot();
  let health = "unreachable";
  try {
    const response = await fetch("http://127.0.0.1:18990/actuator/health/installation", { signal: AbortSignal.timeout(5_000) });
    health = `${response.status} ${await response.text()}`;
  } catch (error) {
    health = error instanceof Error ? error.message : String(error);
  }
  const checks = await Promise.all(["lib/wepush-next-service.jar", "install", "web/index.html"].map(async (relative) => ({
    path: join(root, relative), present: await exists(join(root, relative)),
  })));
  const output = JSON.stringify({ generatedAt: new Date().toISOString(), status, releaseRoot: root, checks, installationHealth: health }, null, 2);
  return { ok: status.installed && checks.every((item) => item.present), message: "本机诊断已完成（不含 Token 和 Secret）", output };
}

async function selectAndStagePlugin(): Promise<CommandResult & { name?: string }> {
  const selected = await dialog.showOpenDialog({ properties: ["openFile"], filters: [{ name: "Signed Provider plugin", extensions: ["zip"] }] });
  if (selected.canceled || !selected.filePaths[0]) return { ok: false, message: "已取消选择", output: "" };
  const archive = selected.filePaths[0];
  const result = await runPluginScript("stage", archive);
  const staged = /staged ([A-Za-z0-9._-]+\.zip)/i.exec(result.output)?.[1];
  return { ...result, name: result.ok ? staged : undefined };
}

async function pluginAction(action: "activate" | "rollback", name: string): Promise<CommandResult> {
  const safeName = name.split(/[\\/]/).pop() ?? "";
  if (!safeName.endsWith(".zip") || safeName !== name) return { ok: false, message: "无效的插件文件名", output: "" };
  return runPluginScript(action, safeName);
}

async function runPluginScript(action: "stage" | "activate" | "rollback", value: string): Promise<CommandResult> {
  const root = releaseRoot();
  if (process.platform === "win32") {
    const script = join(root, "plugins", `${action}.ps1`);
    const parameter = action === "stage" ? "Package" : "Name";
    return elevatedPowerShell(`& '${powerShellString(script)}' -${parameter} '${powerShellString(value)}'`);
  }
  const script = join(root, "plugins", `${action}.sh`);
  if (!await exists(script)) return { ok: false, message: `未找到插件工具：${script}`, output: "" };
  if (process.platform === "darwin") {
    const command = `${shellQuote(script)} ${shellQuote(value)}`;
    return run("osascript", ["-e", `do shell script "${appleScriptString(command)}" with administrator privileges`], 120_000);
  }
  const pkexec = await executable("pkexec");
  if (!pkexec) return { ok: false, message: "未找到 pkexec；请在终端用 sudo 执行插件工具", output: "" };
  return run(pkexec, [script, value], 120_000);
}

async function elevatedPowerShell(body: string): Promise<CommandResult> {
  const id = randomUUID();
  const script = join(tmpdir(), `wepush-next-${id}.ps1`);
  const output = join(tmpdir(), `wepush-next-${id}.log`);
  const content = `$ErrorActionPreference='Stop'\ntry { & { ${body} } *>&1 | Out-File -LiteralPath '${powerShellString(output)}' -Encoding utf8; exit 0 } catch { $_ | Out-File -LiteralPath '${powerShellString(output)}' -Encoding utf8; exit 1 }\n`;
  await writeFile(script, content, { mode: 0o600 });
  try {
    const bootstrap = `$p=Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File','\"${powerShellString(script)}\"') -Wait -PassThru;exit $p.ExitCode`;
    const result = await run("powershell.exe", ["-NoProfile", "-NonInteractive", "-Command", bootstrap], 120_000);
    const captured = await readFile(output, "utf8").catch(() => "");
    return { ok: result.ok, message: captured.trim() || result.message, output: captured.trim() || result.output };
  } finally {
    await rm(script, { force: true });
    await rm(output, { force: true });
  }
}

function releaseRoot(): string {
  if (process.env.WEPUSH_DESKTOP_RELEASE_ROOT) return resolve(process.env.WEPUSH_DESKTOP_RELEASE_ROOT);
  if (process.platform === "win32") return join(process.env.ProgramFiles ?? "C:\\Program Files", "WePush Next", "current");
  return process.platform === "darwin" ? "/Library/WePushNext/current" : "/opt/wepush-next/current";
}

async function tailFiles(paths: string[]): Promise<CommandResult> {
  const sections: string[] = [];
  for (const path of paths) {
    try {
      const lines = (await readFile(path, "utf8")).split(/\r?\n/).slice(-120).join("\n");
      sections.push(`=== ${path} ===\n${lines}`);
    } catch (error) {
      sections.push(`=== ${path} ===\n${error instanceof Error ? error.message : String(error)}`);
    }
  }
  return { ok: sections.some((section) => !section.includes("ENOENT")), message: "已读取最近日志", output: sections.join("\n") };
}

async function executable(name: string): Promise<string | undefined> {
  const result = await run("sh", ["-c", `command -v ${shellQuote(name)}`]);
  return result.ok ? result.output.trim() : undefined;
}

async function exists(path: string): Promise<boolean> {
  try { await access(path); return true; } catch { return false; }
}

function shellQuote(value: string): string { return `'${value.replaceAll("'", "'\\''")}'`; }
function powerShellString(value: string): string { return value.replaceAll("'", "''"); }
function appleScriptString(value: string): string { return value.replaceAll("\\", "\\\\").replaceAll('"', '\\"'); }
function firstLines(value: string, count: number): string { return value.split(/\r?\n/).slice(0, count).join("\n"); }

app.whenReady().then(async () => {
  registerIpcHandlers();
  if (app.isPackaged) {
    protocol.handle("wepush", (request) => {
      const url = new URL(request.url);
      if (url.hostname !== "app") {
        return new Response("Not found", { status: 404 });
      }
      const relativePath = decodeURIComponent(url.pathname).replace(/^\/+/, "") || "index.html";
      const requestedPath = resolve(rendererRoot, relativePath);
      if (requestedPath !== rendererRoot && !requestedPath.startsWith(rendererRoot + sep)) {
        return new Response("Forbidden", { status: 403 });
      }
      return net.fetch(pathToFileURL(requestedPath).toString());
    });
  }
  session.defaultSession.setPermissionRequestHandler((_webContents, _permission, callback) => callback(false));
  await createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) void createWindow();
  });
}).catch((error: unknown) => {
  console.error("Unable to start WePush Next Desktop", error);
  app.exit(1);
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
