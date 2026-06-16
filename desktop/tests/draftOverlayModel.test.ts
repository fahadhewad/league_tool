import { describe, expect, it } from "vitest";

import { buildDraftOverlayModel } from "../src/core/champSelect/draftOverlayModel";
import type { PickRecommendations, WinProbability } from "../src/core/backend/types";

const championNames = { 64: "Lee Sin", 222: "Jinx", 238: "Zed", 51: "Caitlyn" };

describe("buildDraftOverlayModel", () => {
  it("resolves names, win probability and top picks", () => {
    const recommendations: PickRecommendations = {
      role: "UTILITY",
      recommendations: [
        { championId: 89, name: "Leona", role: "UTILITY", score: 68, synergyScore: 3, counterScore: 3, damageFitScore: 4, reasons: ["Brings CC"] },
        { championId: 111, name: "Nautilus", role: "UTILITY", score: 67, synergyScore: 3, counterScore: 3, damageFitScore: 3, reasons: ["Front line"] },
      ],
    };
    const winProbability: WinProbability = { winProbability: 0.62, modelLoaded: true, source: "ml-model" };

    const model = buildDraftOverlayModel({
      participants: [
        { championId: 64, team: 0 },
        { championId: 222, team: 0 },
        { championId: 0, team: 0 }, // not locked in yet
        { championId: 238, team: 1 },
        { championId: 51, team: 1 },
      ],
      recommendations,
      winProbability,
      championNames,
    });

    expect(model.role).toBe("UTILITY");
    expect(model.allies).toEqual(["Lee Sin", "Jinx", "Picking…"]);
    expect(model.enemies).toEqual(["Zed", "Caitlyn"]);
    expect(model.winProbabilityPct).toBe(62);
    expect(model.winProbabilitySource).toBe("ml-model");
    expect(model.topPicks.map((p) => p.name)).toEqual(["Leona", "Nautilus"]);
  });

  it("handles missing recommendations and win probability", () => {
    const model = buildDraftOverlayModel({
      participants: [{ championId: 64, team: 0 }],
      recommendations: null,
      winProbability: null,
      championNames,
    });
    expect(model.winProbabilityPct).toBeNull();
    expect(model.topPicks).toEqual([]);
    expect(model.role).toBeNull();
  });
});
