import { describe, expect, it } from "vitest";

import { buildLiveOverlayModel, formatClock } from "../src/core/liveclient/liveOverlayModel";
import type { LiveEvent, LiveGameStats, LivePlayer } from "../src/core/liveclient/liveClient";

describe("formatClock", () => {
  it("formats seconds as m:ss", () => {
    expect(formatClock(0)).toBe("0:00");
    expect(formatClock(65)).toBe("1:05");
    expect(formatClock(600)).toBe("10:00");
    expect(formatClock(-5)).toBe("0:00");
  });
});

describe("buildLiveOverlayModel", () => {
  const stats: LiveGameStats = { gameMode: "CLASSIC", gameTime: 754 };
  const players: LivePlayer[] = [
    { championName: "Garen", summonerName: "A", team: "ORDER", position: "TOP", scores: { kills: 3, deaths: 1, assists: 2, creepScore: 120 } },
    { championName: "Jinx", summonerName: "B", team: "ORDER", position: "BOTTOM", scores: { kills: 5, deaths: 0, assists: 1, creepScore: 200 } },
    { championName: "Zed", summonerName: "C", team: "CHAOS", position: "MIDDLE", scores: { kills: 4, deaths: 2, assists: 0, creepScore: 150 } },
  ];
  const events: LiveEvent[] = [
    { EventID: 1, EventName: "GameStart", EventTime: 0 },
    { EventID: 2, EventName: "DragonKill", EventTime: 300 },
    { EventID: 3, EventName: "TurretKilled", EventTime: 480 },
    { EventID: 4, EventName: "DragonKill", EventTime: 700 },
    { EventID: 5, EventName: "BaronKill", EventTime: 720 },
  ];

  it("derives clock, objective counts and team split", () => {
    const model = buildLiveOverlayModel(stats, players, events);
    expect(model.clock).toBe("12:34");
    expect(model.objectives).toEqual({ dragons: 2, barons: 1, heralds: 0, towers: 1 });
    expect(model.teams.order).toHaveLength(2);
    expect(model.teams.chaos).toHaveLength(1);
    expect(model.teams.order[0]).toMatchObject({ champion: "Garen", kda: "3/1/2", cs: 120 });
  });

  it("lists recent events newest-first up to the limit", () => {
    const model = buildLiveOverlayModel(stats, players, events, 2);
    expect(model.recentEvents).toEqual(["12:00  BaronKill", "11:40  DragonKill"]);
  });
});
