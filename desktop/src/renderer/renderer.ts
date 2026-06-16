/** Minimal renderer logic for the main window. */
import type { ChampSelectPayload } from "../main/preload";

interface LeagueToolApi {
  onChampSelect(callback: (payload: ChampSelectPayload) => void): () => void;
  getProfile(platform: string, gameName: string, tagLine: string): Promise<unknown>;
}

declare global {
  interface Window {
    leaguetool: LeagueToolApi;
  }
}

const status = document.getElementById("status");
const output = document.getElementById("output");

window.leaguetool.onChampSelect((payload) => {
  if (status) {
    status.textContent = "In champ select";
  }
  if (output) {
    output.textContent = JSON.stringify(payload, null, 2);
  }
});

const form = document.getElementById("profile-form") as HTMLFormElement | null;
form?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = new FormData(form);
  const profile = await window.leaguetool.getProfile(
    String(data.get("platform")),
    String(data.get("gameName")),
    String(data.get("tagLine")),
  );
  if (output) {
    output.textContent = JSON.stringify(profile, null, 2);
  }
});

export {};
