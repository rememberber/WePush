import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("wepushDesktop", Object.freeze({
  platform: process.platform,
  versions: Object.freeze({
    chrome: process.versions.chrome,
    electron: process.versions.electron,
  }),
  token: Object.freeze({
    load: () => ipcRenderer.invoke("wepush:token:load") as Promise<string>,
    save: (token: string) => ipcRenderer.invoke("wepush:token:save", token) as Promise<void>,
    clear: () => ipcRenderer.invoke("wepush:token:clear") as Promise<void>,
  }),
  service: Object.freeze({
    status: () => ipcRenderer.invoke("wepush:service:status"),
    start: () => ipcRenderer.invoke("wepush:service:start"),
    stop: () => ipcRenderer.invoke("wepush:service:stop"),
    logs: () => ipcRenderer.invoke("wepush:service:logs"),
    diagnose: () => ipcRenderer.invoke("wepush:service:diagnose"),
  }),
  plugins: Object.freeze({
    selectAndStage: () => ipcRenderer.invoke("wepush:plugin:select-stage"),
    activate: (name: string) => ipcRenderer.invoke("wepush:plugin:activate", name),
    rollback: (name: string) => ipcRenderer.invoke("wepush:plugin:rollback", name),
  }),
}));
