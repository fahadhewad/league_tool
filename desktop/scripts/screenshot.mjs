/**
 * Headless screenshot harness: launches the real renderer/overlay windows on a virtual display and
 * feeds them sample champ-select / in-game view-models (the same IPC the live app uses), then
 * captures PNGs. Lets you preview the UI without a League client. Run: xvfb-run -a electron scripts/screenshot.mjs
 */
import { app, BrowserWindow } from "electron";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

app.disableHardwareAcceleration();
app.commandLine.appendSwitch("no-sandbox");

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..");
const preload = join(root, "dist/main/preload.js");
const out = join(root, "shots");

const draft = {
  role: "UTILITY",
  winProbabilityPct: 54,
  winProbabilitySource: "ml-model",
  allies: ["Lee Sin", "Jinx", "Picking…"],
  enemies: ["Zed", "Caitlyn"],
  topPicks: [
    { name: "Leona", score: 68, reasons: ["Brings crowd control your comp lacks"] },
    { name: "Nautilus", score: 67, reasons: ["Adds a front line your comp is missing"] },
    { name: "Thresh", score: 66, reasons: ["Favoured into enemy Zed"] },
  ],
};

const live = {
  gameMode: "CLASSIC",
  clock: "21:30",
  objectives: { dragons: 2, barons: 1, heralds: 1, towers: 4 },
  teams: { order: [], chaos: [] },
  recentEvents: ["21:05  BaronKill", "18:40  DragonKill", "14:12  TurretKilled"],
};

const wait = (ms) => new Promise((r) => setTimeout(r, ms));

async function shoot(win, file) {
  const image = await win.capturePage();
  const { writeFileSync, mkdirSync } = await import("node:fs");
  mkdirSync(out, { recursive: true });
  writeFileSync(join(out, file), image.toPNG());
  console.log("wrote", file);
}

app.whenReady().then(async () => {
  // Overlay (transparent card)
  const overlay = new BrowserWindow({
    width: 420, height: 460, show: false, frame: false, transparent: true,
    webPreferences: { preload, contextIsolation: true, sandbox: false },
  });
  await overlay.loadFile(join(root, "dist/renderer/overlay.html"));
  await wait(600);
  overlay.webContents.send("champ-select", draft);
  await wait(400);
  await shoot(overlay, "overlay-draft.png");
  overlay.webContents.send("in-game", live);
  await wait(400);
  await shoot(overlay, "overlay-ingame.png");

  // Main window
  const main = new BrowserWindow({
    width: 1100, height: 760, show: false, backgroundColor: "#0f1420",
    webPreferences: { preload, contextIsolation: true, sandbox: false },
  });
  await main.loadFile(join(root, "dist/renderer/index.html"));
  await wait(600);
  main.webContents.send("champ-select", draft);
  await wait(400);
  await shoot(main, "main-draft.png");

  app.quit();
});
