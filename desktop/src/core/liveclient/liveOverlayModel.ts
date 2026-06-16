/**
 * Pure view-model builder for the in-game overlay. Turns the raw Live Client Data API responses
 * into a compact, render-ready shape. Kept free of DOM/Electron so it can be unit-tested.
 *
 * In-game scoreboard names are visible on screen in the player's own client, so (unlike ranked
 * champ select) there is no anonymity gate to apply here.
 */
import type { LiveEvent, LiveGameStats, LivePlayer } from "./liveClient";

export interface LivePlayerView {
  champion: string;
  position: string;
  kda: string;
  cs: number;
}

export interface LiveOverlayModel {
  gameMode: string;
  clock: string;
  objectives: { dragons: number; barons: number; heralds: number; towers: number };
  teams: { order: LivePlayerView[]; chaos: LivePlayerView[] };
  recentEvents: string[];
}

export function formatClock(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds));
  const mins = Math.floor(total / 60);
  const secs = total % 60;
  return `${mins}:${secs.toString().padStart(2, "0")}`;
}

function toView(player: LivePlayer): LivePlayerView {
  const s = player.scores;
  return {
    champion: player.championName,
    position: player.position ?? "",
    kda: s ? `${s.kills}/${s.deaths}/${s.assists}` : "0/0/0",
    cs: s?.creepScore ?? 0,
  };
}

function countEvents(events: LiveEvent[], names: string[]): number {
  return events.filter((e) => names.includes(e.EventName)).length;
}

export function buildLiveOverlayModel(
  stats: LiveGameStats,
  players: LivePlayer[],
  events: LiveEvent[],
  recentLimit = 5,
): LiveOverlayModel {
  const order = players.filter((p) => p.team === "ORDER").map(toView);
  const chaos = players.filter((p) => p.team === "CHAOS").map(toView);

  return {
    gameMode: stats.gameMode,
    clock: formatClock(stats.gameTime),
    objectives: {
      dragons: countEvents(events, ["DragonKill"]),
      barons: countEvents(events, ["BaronKill"]),
      heralds: countEvents(events, ["HeraldKill", "RiftHerald"]),
      towers: countEvents(events, ["TurretKilled"]),
    },
    teams: { order, chaos },
    recentEvents: events
      .slice(-recentLimit)
      .reverse()
      .map((e) => `${formatClock(e.EventTime)}  ${e.EventName}`),
  };
}
