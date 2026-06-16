/** Safe, minimal bridge between the renderer and the main process (context isolation on). */
import { contextBridge, ipcRenderer, type IpcRendererEvent } from "electron";

import type { DraftOverlayModel } from "../core/champSelect/draftOverlayModel";
import type { LiveOverlayModel } from "../core/liveclient/liveOverlayModel";

export type { DraftOverlayModel, LiveOverlayModel };

/** The API exposed on `window.leaguetool` in renderer windows. */
export interface LeagueToolApi {
  onChampSelect(callback: (payload: DraftOverlayModel) => void): () => void;
  onInGame(callback: (payload: LiveOverlayModel) => void): () => void;
  getProfile(platform: string, gameName: string, tagLine: string): Promise<unknown>;
}

declare global {
  interface Window {
    leaguetool: LeagueToolApi;
  }
}

function subscribe<T>(channel: string, callback: (payload: T) => void): () => void {
  const listener = (_event: IpcRendererEvent, payload: T) => callback(payload);
  ipcRenderer.on(channel, listener);
  return () => ipcRenderer.removeListener(channel, listener);
}

contextBridge.exposeInMainWorld("leaguetool", {
  onChampSelect(callback: (payload: DraftOverlayModel) => void): () => void {
    return subscribe("champ-select", callback);
  },
  onInGame(callback: (payload: LiveOverlayModel) => void): () => void {
    return subscribe("in-game", callback);
  },
  getProfile(platform: string, gameName: string, tagLine: string): Promise<unknown> {
    return ipcRenderer.invoke("profile:get", { platform, gameName, tagLine });
  },
});
