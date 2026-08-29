import { access, chmod, cp, mkdir, readFile, rename, rm, writeFile } from "node:fs/promises";
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
const repositoryRoot = join(appRoot, "..", "..", "..", "..");
const nextRoot = join(repositoryRoot, "next");
const desktopMetadata = JSON.parse(await readFile(join(appRoot, "package.json"), "utf8"));

function macBundleVersions(version) {
  const match = /^(\d+)\.(\d+)\.(\d+)(?:-(alpha|beta|rc)\.(\d+))?$/.exec(version);
  if (!match) throw new Error(`Unsupported Desktop version for macOS packaging: ${version}`);
  const [, major, minor, patch, channel, iteration] = match;
  if (!channel) return { marketing: `${major}.${minor}.${patch}`, build: `${major}.${minor}.${patch}` };
  const channelOffset = { alpha: 10_000, beta: 20_000, rc: 30_000 }[channel];
  const build = Number(patch) * 100_000 + channelOffset + Number(iteration);
  return { marketing: `${major}.${minor}.${patch}`, build: `${major}.${minor}.${build}` };
}

try {
  await access(electronDist);
} catch {
  console.log("Electron runtime is missing; installing the pinned runtime before packaging");
  await execFileAsync(process.execPath, [join(electronRoot, "install.js")], { cwd: electronRoot });
  await access(electronDist);
}

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
  const executableName = "WePush Next";
  await rename(join(destination, "Contents", "MacOS", "Electron"),
    join(destination, "Contents", "MacOS", executableName));
  const versions = macBundleVersions(desktopMetadata.version);
  const plistValues = {
    CFBundleDisplayName: "WePush Next",
    CFBundleExecutable: executableName,
    CFBundleIdentifier: "com.fangxuele.wepush.next.desktop",
    CFBundleName: "WePush Next",
    CFBundleShortVersionString: versions.marketing,
    CFBundleVersion: versions.build,
  };
  for (const [key, value] of Object.entries(plistValues)) {
    await execFileAsync("plutil", ["-replace", key, "-string", value, plistPath]);
  }
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
await cp(join(repositoryRoot, "LICENSE.txt"), join(resources, "LICENSE.txt"));
await cp(join(nextRoot, "UNSIGNED-NOTICE.md"), join(resources, "UNSIGNED-NOTICE.md"));
await cp(join(nextRoot, "SECURITY.md"), join(resources, "SECURITY.md"));
await cp(join(nextRoot, "THIRD-PARTY-NOTICES.md"), join(resources, "THIRD-PARTY-NOTICES.md"));
await writeFile(join(packagedApp, "package.json"), JSON.stringify({
  name: "wepush-next-desktop", version: desktopMetadata.version, private: true,
  type: "module", main: "dist/main/index.js",
}, null, 2));
if (process.platform === "darwin") {
  const identity = process.env.WEPUSH_CODESIGN_IDENTITY?.trim() || "-";
  const arguments_ = ["--force", "--deep", "--sign", identity];
  if (identity !== "-") arguments_.push("--options", "runtime", "--timestamp");
  await execFileAsync("codesign", [...arguments_, destination]);
}
console.log(`Packaged WePush Next Desktop: ${destination}`);
