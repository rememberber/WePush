import { chmod, cp, mkdir, readFile, rename, rm, writeFile } from "node:fs/promises";
import { execFile } from "node:child_process";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const require = createRequire(import.meta.url);
const execFileAsync = promisify(execFile);
const appRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
const electronRoot = dirname(require.resolve("electron/package.json"));
const electronDist = join(electronRoot, "dist");
const releaseRoot = join(appRoot, "release");
const webDist = join(appRoot, "..", "web", "dist");

await rm(releaseRoot, { recursive: true, force: true });
await mkdir(releaseRoot, { recursive: true });

let destination;
let resources;
if (process.platform === "darwin") {
  destination = join(releaseRoot, "WePush Next.app");
  await cp(join(electronDist, "Electron.app"), destination, {
    recursive: true,
    verbatimSymlinks: true,
  });
  resources = join(destination, "Contents", "Resources");
  const plistPath = join(destination, "Contents", "Info.plist");
  const plist = (await readFile(plistPath, "utf8"))
    .replaceAll("com.github.Electron", "com.fangxuele.wepush.next.desktop")
    .replaceAll("Electron", "WePush Next");
  await writeFile(plistPath, plist);
} else {
  const directory = process.platform === "win32" ? "win-unpacked" : "linux-unpacked";
  destination = join(releaseRoot, directory);
  await cp(electronDist, destination, { recursive: true, verbatimSymlinks: true });
  resources = join(destination, "resources");
  if (process.platform === "win32") await rename(join(destination, "electron.exe"), join(destination, "WePush Next.exe"));
  else {
    await rename(join(destination, "electron"), join(destination, "wepush-next"));
    await chmod(join(destination, "wepush-next"), 0o755);
  }
}

const packagedApp = join(resources, "app");
await mkdir(packagedApp, { recursive: true });
await cp(join(appRoot, "dist"), join(packagedApp, "dist"), { recursive: true });
await cp(webDist, join(resources, "web", "dist"), { recursive: true });
await writeFile(join(packagedApp, "package.json"), JSON.stringify({
  name: "wepush-next-desktop", version: "0.1.0", private: true,
  type: "module", main: "dist/main/index.js",
}, null, 2));
if (process.platform === "darwin") {
  const identity = process.env.WEPUSH_CODESIGN_IDENTITY?.trim() || "-";
  const arguments_ = ["--force", "--deep", "--sign", identity];
  if (identity !== "-") arguments_.push("--options", "runtime", "--timestamp");
  await execFileAsync("codesign", [...arguments_, destination]);
}
console.log(`Packaged WePush Next Desktop: ${destination}`);
