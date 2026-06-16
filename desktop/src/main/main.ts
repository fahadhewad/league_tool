/**
 * Electron main process. Wires the compliant LCU/Live-Client readers to the backend and pushes
 * results to the main window and a transparent overlay.
 *
 * Compliance: champ-select participants are passed through {@link anonymizeChampSelect} before they
 * are ever sent to a renderer, so non-party identities in ranked are obfuscated at the source.
 */
import { app, BrowserWindow, ipcMain } from "electron";
import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { Agent } from "undici";

import { BackendClient } from "../core/backend/backendClient";
import { toRecommendInput } from "../core/champSelect/champSelectModel";
import {
  anonymizeChampSelect,
  type ChampSelectParticipant,
} from "../core/compliance/rankedAnonymizer";
import { LcuClient } from "../core/lcu/lcuClient";
import { defaultLockfilePaths, parseLockfile, type Lockfile } from "../core/lcu/lockfile";

const moduleDir = dirname(fileURLToPath(import.meta.url));

// The LCU/Live-Client APIs use a self-signed certificate; accept it for localhost only.
const insecureAgent = new Agent({ connect: { rejectUnauthorized: false } });
const localFetch = (url: string, init: RequestInit = {}): Promise<Response> =>
  fetch(url, { ...init, dispatcher: insecureAgent } as RequestInit);

const backend = new BackendClient({ baseUrl: process.env.BACKEND_URL ?? "http://localhost:8080" });

let mainWindow: BrowserWindow | null = null;
let overlayWindow: BrowserWindow | null = null;

function createWindows(): void {
  mainWindow = new BrowserWindow({
    width: 1100,
    height: 760,
    webPreferences: {
      preload: join(moduleDir, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });
  void mainWindow.loadFile(join(moduleDir, "../renderer/index.html"));

  overlayWindow = new BrowserWindow({
    width: 420,
    height: 600,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    focusable: false,
    webPreferences: { preload: join(moduleDir, "preload.js"), contextIsolation: true },
  });
  void overlayWindow.loadFile(join(moduleDir, "../renderer/overlay.html"));
  overlayWindow.setIgnoreMouseEvents(true, { forward: true });
}

async function readLockfile(): Promise<Lockfile | null> {
  for (const path of defaultLockfilePaths()) {
    try {
      return parseLockfile(await readFile(path, "utf8"));
    } catch {
      // not at this path; try the next
    }
  }
  return null;
}

async function pollChampSelect(): Promise<void> {
  const lockfile = await readLockfile();
  if (!lockfile) {
    return; // client not running
  }
  const lcu = new LcuClient(lockfile, localFetch);
  let session;
  let summoner;
  let queueId;
  try {
    [session, summoner, queueId] = await Promise.all([
      lcu.getChampSelectSession(),
      lcu.getCurrentSummoner(),
      lcu.getQueueId(),
    ]);
  } catch {
    return; // not currently in champ select
  }

  const participants: ChampSelectParticipant[] = [
    ...session.myTeam.map((c) => ({ ...c, team: 0 })),
    ...session.theirTeam.map((c) => ({ ...c, team: 1 })),
  ];
  const safeParticipants = anonymizeChampSelect(participants, {
    queueId: queueId ?? 0,
    localPuuid: summoner.puuid,
    localCellId: session.localPlayerCellId,
  });

  const input = toRecommendInput(session);
  const recommendations = input.role
    ? await backend
        .recommend({ role: input.role, allies: input.allies, enemies: input.enemies, bans: input.bans })
        .catch(() => null)
    : null;
  const winProbability = await backend
    .winProbability({ allies: input.allies, enemies: input.enemies })
    .catch(() => null);

  const payload = { participants: safeParticipants, recommendations, winProbability };
  mainWindow?.webContents.send("champ-select", payload);
  overlayWindow?.webContents.send("champ-select", payload);
}

app.whenReady().then(() => {
  createWindows();
  setInterval(() => void pollChampSelect(), 2000);
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindows();
    }
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});

ipcMain.handle(
  "profile:get",
  (_event, args: { platform: string; gameName: string; tagLine: string }) =>
    backend.getProfile(args.platform, args.gameName, args.tagLine),
);
