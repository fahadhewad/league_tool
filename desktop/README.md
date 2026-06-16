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
src/renderer/    UI (main window + transparent overlay)
tests/           vitest suite for the core logic
```

## Develop
```bash
npm install                 # add ELECTRON_SKIP_BINARY_DOWNLOAD=1 to skip the Electron binary in CI
npm test                    # vitest (core logic)
npm run typecheck           # tsc --noEmit
npm run build && npm run dev # build + launch Electron (requires the League client + backend running)
```

The overlay and LCU/Live-Client features require a real League client; the core logic is fully
testable headlessly, which is what CI runs.
