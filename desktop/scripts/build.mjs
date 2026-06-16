/**
 * Bundle the Electron entry points with esbuild and copy the renderer assets into dist/.
 *
 * Bundling (rather than bare `tsc`) gives self-contained output with no extensionless-import
 * resolution problems at runtime, and is what electron-builder then packages.
 */
import { build } from "esbuild";
import { cpSync, mkdirSync, rmSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const outdir = join(root, "dist");

rmSync(outdir, { recursive: true, force: true }); // start clean (drop any stale tsc output)

const common = { bundle: true, format: "esm", sourcemap: true, logLevel: "info", target: "es2022" };

await build({
  ...common,
  platform: "node",
  external: ["electron"],
  entryPoints: {
    "main/main": join(root, "src/main/main.ts"),
    "main/preload": join(root, "src/main/preload.ts"),
  },
  outdir,
});

await build({
  ...common,
  platform: "browser",
  entryPoints: {
    "renderer/renderer": join(root, "src/renderer/renderer.ts"),
    "renderer/overlay": join(root, "src/renderer/overlay.ts"),
  },
  outdir,
});

// Static renderer assets (HTML/CSS) live next to the compiled renderer bundles.
mkdirSync(join(outdir, "renderer"), { recursive: true });
for (const asset of ["index.html", "overlay.html", "styles.css"]) {
  cpSync(join(root, "src/renderer", asset), join(outdir, "renderer", asset));
}

console.log("Build complete -> dist/");
