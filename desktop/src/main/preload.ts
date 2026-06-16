/** Safe, minimal bridge between the renderer and the main process (context isolation on). */
import { contextBridge, ipcRenderer, type IpcRendererEvent } from "electron";

export interface ChampSelectPayload {
  participants: unknown[];
  recommendations: unknown;
  winProbability: unknown;
}

contextBridge.exposeInMainWorld("leaguetool", {
  onChampSelect(callback: (payload: ChampSelectPayload) => void): () => void {
    const listener = (_event: IpcRendererEvent, payload: ChampSelectPayload) => callback(payload);
    ipcRenderer.on("champ-select", listener);
    return () => ipcRenderer.removeListener("champ-select", listener);
  },
  getProfile(platform: string, gameName: string, tagLine: string): Promise<unknown> {
    return ipcRenderer.invoke("profile:get", { platform, gameName, tagLine });
  },
});
