import { expect, it } from "vitest";
import type { LcuChampSelectSession } from "../src/core/lcu/lcuClient";
import { positionToRole, toRecommendInput } from "../src/core/champSelect/champSelectModel";

const session: LcuChampSelectSession = {
  localPlayerCellId: 0,
  myTeam: [
    { cellId: 0, championId: 0, assignedPosition: "utility", puuid: "me" },
    { cellId: 1, championId: 222, assignedPosition: "bottom", puuid: "adc" },
    { cellId: 2, championId: 64, assignedPosition: "jungle", puuid: "jg" },
  ],
  theirTeam: [
    { cellId: 5, championId: 238, assignedPosition: "middle" },
    { cellId: 6, championId: 0, assignedPosition: "top" },
  ],
  bans: { myTeamBans: [105], theirTeamBans: [55, 0] },
};

it("maps positions to backend roles", () => {
  expect(positionToRole("utility")).toBe("UTILITY");
  expect(positionToRole("BOTTOM")).toBe("BOTTOM");
  expect(positionToRole("")).toBeNull();
  expect(positionToRole("fill")).toBeNull();
});

it("builds a recommend input excluding the local player and unpicked cells", () => {
  const input = toRecommendInput(session);

  expect(input.role).toBe("UTILITY");
  expect(input.allies).toEqual([
    { championId: 222, role: "BOTTOM" },
    { championId: 64, role: "JUNGLE" },
  ]);
  expect(input.enemies).toEqual([{ championId: 238, role: "MIDDLE" }]);
  expect(input.bans).toEqual([105, 55]); // zeros filtered out
});
