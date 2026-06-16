# LeagueTool desktop (Electron)

The desktop client and in-game overlay. It reads the League Client through official local APIs only
— **no memory reading, no injection** — and renders draft advice + an overlay.

## Compliance first
`src/core/compliance/rankedAnonymizer.ts` is the hard gate: in ranked Solo/Flex champ select it
replaces every non-party participant's name with `Ally #n` / `Enemy #n` **and strips their
identifiers** before anything reaches a renderer. Champion picks are kept (a champion is not an
identity). Names pass through unchanged only in queues where the client already shows them
(normal/draft). See [`../docs/COMPLIANCE.md`](../docs/COMPLIANCE.md).

## Layout
```
src/core/        pure, unit-tested logic (no Electron imports)
  compliance/    queue detection + ranked anonymizer
  lcu/           lockfile parsing + LCU REST client
  liveclient/    Live Client Data API client
  champSelect/   LCU session -> backend draft model
  backend/       Spring backend client + API types
src/main/        Electron main process + preload
src/renderer/    UI: main window (renderer.ts) + transparent overlay (overlay.ts)
scripts/         esbuild bundler (build.mjs)
tests/           vitest suite for the core logic
```

## What it shows
- **Champ select:** the backend's pick recommendations (with reasons), a win-probability bar, and
  the anonymized teams — pushed to a transparent, click-through overlay and the main window.
- **In game:** the main process polls the Live Client Data API and the overlay shows the clock,
  objective counts (dragons/barons/heralds/towers), and recent events.

The render-ready shapes are built by pure, unit-tested functions (`draftOverlayModel`,
`liveOverlayModel`); the renderer just paints them (via `textContent`, under a strict CSP).

## Develop
```bash
npm install                 # add ELECTRON_SKIP_BINARY_DOWNLOAD=1 to skip the Electron binary in CI
npm test                    # vitest (core logic)
npm run typecheck           # tsc --noEmit
npm run build               # esbuild -> dist/ (bundled main, preload, renderers + copied HTML/CSS)
npm run dev                 # build + launch Electron (requires the League client + backend running)
npm run package             # build installers via electron-builder (run on the target OS)
```

The overlay and LCU/Live-Client features require a real League client, and installers are produced
on the OS you target; the core logic is fully testable headlessly, which is what CI runs.

## Building the Windows installer (+ uninstaller)
Run these **on a Windows PC** (Node 20+ installed), from the `desktop/` folder:
```powershell
npm install
npm run package
```
This produces `desktop\release\LeagueTool Setup <version>.exe`. That single installer **also
installs an uninstaller** — there is no separate uninstaller to build:
- it registers LeagueTool in **Settings → Apps / Add-or-Remove Programs**, and
- writes `Uninstall LeagueTool.exe` into the install folder.

Uninstalling from either place removes the app and (per `deleteAppDataOnUninstall`) its app data.
The installer lets you choose the install directory and creates Start-menu + desktop shortcuts
(`nsis` settings in `package.json`).

> Optional: drop a `build/icon.ico` (256×256) before packaging to brand the installer/app;
> otherwise the default Electron icon is used. For other targets: `.dmg` builds on macOS, and
> `AppImage`/`.deb` build on Linux (`npm run package`).
