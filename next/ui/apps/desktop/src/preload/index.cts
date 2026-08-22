import { contextBridge } from "electron";

contextBridge.exposeInMainWorld("wepushDesktop", Object.freeze({
  platform: process.platform,
  versions: Object.freeze({
    chrome: process.versions.chrome,
    electron: process.versions.electron,
  }),
}));
