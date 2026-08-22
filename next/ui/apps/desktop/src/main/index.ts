import { app, BrowserWindow, net, protocol, session, shell } from "electron";
import { dirname, join, resolve, sep } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const currentDirectory = dirname(fileURLToPath(import.meta.url));
const developmentUrl = process.env.WEPUSH_UI_URL ?? "http://127.0.0.1:5173";
const productionOrigin = "wepush://app";
const rendererRoot = resolve(currentDirectory, "../../../web/dist");

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
    const expectedOrigin = app.isPackaged ? productionOrigin : new URL(developmentUrl).origin;
    if (!url.startsWith(expectedOrigin)) {
      event.preventDefault();
    }
  });
  window.once("ready-to-show", () => window.show());

  if (app.isPackaged) {
    await window.loadURL(`${productionOrigin}/index.html`);
  } else {
    await window.loadURL(developmentUrl);
  }
}

app.whenReady().then(async () => {
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
