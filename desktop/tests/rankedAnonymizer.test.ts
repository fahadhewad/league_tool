import { describe, expect, it } from "vitest";
import { anonymizeChampSelect, type ChampSelectParticipant } from "../src/core/compliance/rankedAnonymizer";
import { RANKED_SOLO_DUO } from "../src/core/compliance/queue";

const participants: ChampSelectParticipant[] = [
  { cellId: 0, team: 0, puuid: "me", summonerId: 1, displayName: "Me", championId: 64 },
  { cellId: 1, team: 0, puuid: "mate", summonerId: 2, displayName: "Premade", championId: 89 },
  { cellId: 2, team: 0, puuid: "rand1", summonerId: 3, displayName: "RandomAlly", championId: 222 },
  { cellId: 5, team: 1, puuid: "enemy1", summonerId: 4, displayName: "EnemyTop", championId: 122 },
  { cellId: 6, team: 1, puuid: "enemy2", summonerId: 5, displayName: "EnemyMid", championId: 238 },
];

describe("rankedAnonymizer", () => {
  it("hides non-party identities in ranked while keeping local and party", () => {
    const out = anonymizeChampSelect(participants, {
      queueId: RANKED_SOLO_DUO,
      localPuuid: "me",
      partyPuuids: ["mate"],
    });

    expect(out.find((p) => p.cellId === 0)?.displayName).toBe("Me");
    expect(out.find((p) => p.cellId === 1)?.displayName).toBe("Premade");

    const randomAlly = out.find((p) => p.cellId === 2)!;
    expect(randomAlly.displayName).toBe("Ally #3");
    expect(randomAlly.puuid).toBeUndefined();
    expect(randomAlly.summonerId).toBeUndefined();
    expect(randomAlly.championId).toBe(222); // champion preserved

    const enemy = out.find((p) => p.cellId === 5)!;
    expect(enemy.displayName).toBe("Enemy #1");
    expect(enemy.puuid).toBeUndefined();
  });

  it("passes through names in non-ranked queues", () => {
    const out = anonymizeChampSelect(participants, { queueId: 400, localPuuid: "me" });
    expect(out.find((p) => p.cellId === 2)?.displayName).toBe("RandomAlly");
    expect(out.find((p) => p.cellId === 5)?.puuid).toBe("enemy1");
  });

  it("does not mutate the input", () => {
    const copy = structuredClone(participants);
    anonymizeChampSelect(participants, { queueId: RANKED_SOLO_DUO, localPuuid: "me" });
    expect(participants).toEqual(copy);
  });
});
