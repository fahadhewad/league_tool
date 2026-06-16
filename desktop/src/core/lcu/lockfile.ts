/**
 * League Client (LCU) lockfile handling. The running client writes a lockfile of the form
 * {@code LeagueClient:pid:port:password:protocol}; we read it (never game memory) to talk to the
 * local LCU REST/WebSocket API.
 */
import { homedir, platform } from "node:os";
import { join } from "node:path";

export interface Lockfile {
  processName: string;
  pid: number;
  port: number;
  password: string;
  protocol: string;
}

export function parseLockfile(content: string): Lockfile {
  const parts = content.trim().split(":");
  if (parts.length < 5) {
    throw new Error("Invalid lockfile: expected 'name:pid:port:password:protocol'");
  }
  const [processName, pid, port, password, protocol] = parts;
  const parsedPort = Number(port);
  if (!Number.isInteger(parsedPort) || parsedPort <= 0) {
    throw new Error(`Invalid lockfile port: ${port}`);
  }
  return { processName, pid: Number(pid), port: parsedPort, password, protocol };
}

/** Base URL for LCU REST calls. */
export function lcuBaseUrl(lockfile: Lockfile): string {
  return `${lockfile.protocol}://127.0.0.1:${lockfile.port}`;
}

/** HTTP Basic auth header for the LCU (username is always "riot"). */
export function lcuAuthHeader(lockfile: Lockfile): string {
  return "Basic " + Buffer.from(`riot:${lockfile.password}`).toString("base64");
}

/** Best-effort default lockfile locations per platform. */
export function defaultLockfilePaths(): string[] {
  switch (platform()) {
    case "win32":
      return [
        "C:/Riot Games/League of Legends/lockfile",
        join(process.env.LOCALAPPDATA ?? "", "Riot Games/League of Legends/lockfile"),
      ];
    case "darwin":
      return [
        "/Applications/League of Legends.app/Contents/LoL/lockfile",
        join(homedir(), "Applications/League of Legends.app/Contents/LoL/lockfile"),
      ];
    default:
      return [join(homedir(), ".local/share/leagueoflegends/lockfile")];
  }
}
