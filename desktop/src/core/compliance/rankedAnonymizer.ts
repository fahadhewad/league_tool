/**
 * The compliance gate. In ranked Solo/Flex champ select, every non-party participant's identity is
 * obfuscated to "Ally #n" / "Enemy #n" BEFORE it can reach the UI — and their identifiers (puuid /
 * summonerId / real name) are stripped entirely, so the rest of the app literally cannot look them
 * up. Champion picks are preserved (a champion is not an identity).
 *
 * Outside ranked Solo/Flex (e.g. normal/draft, where names are visible in-client), participants pass
 * through unchanged.
 */
import { isRankedSoloOrFlex } from "./queue";

export interface ChampSelectParticipant {
  cellId: number;
  team: number;
  puuid?: string;
  summonerId?: number;
  displayName?: string;
  gameName?: string;
  championId?: number;
}

export interface AnonymizeContext {
  queueId: number;
  /** Identify the local player by puuid and/or cell id. */
  localPuuid?: string;
  localCellId?: number;
  /** Puuids of your premade party (allowed to remain visible in ranked). */
  partyPuuids?: string[];
}

export function anonymizeChampSelect(
  participants: ChampSelectParticipant[],
  ctx: AnonymizeContext,
): ChampSelectParticipant[] {
  if (!isRankedSoloOrFlex(ctx.queueId)) {
    return participants.map((p) => ({ ...p }));
  }

  const party = new Set(ctx.partyPuuids ?? []);
  const local = participants.find(
    (p) =>
      (ctx.localPuuid !== undefined && p.puuid === ctx.localPuuid) ||
      (ctx.localCellId !== undefined && p.cellId === ctx.localCellId),
  );
  const localTeam = local?.team;

  const indexInTeam = buildTeamIndices(participants);

  return participants.map((p) => {
    const isLocal = local !== undefined && p.cellId === local.cellId;
    const isParty = p.puuid !== undefined && party.has(p.puuid);
    if (isLocal || isParty) {
      return { ...p };
    }
    const label = localTeam !== undefined && p.team === localTeam ? "Ally" : "Enemy";
    const index = indexInTeam.get(p.cellId) ?? p.cellId;
    // Identity fields intentionally dropped: only cell, team, label and champion survive.
    return {
      cellId: p.cellId,
      team: p.team,
      championId: p.championId,
      displayName: `${label} #${index}`,
    };
  });
}

function buildTeamIndices(participants: ChampSelectParticipant[]): Map<number, number> {
  const byTeam = new Map<number, ChampSelectParticipant[]>();
  for (const p of [...participants].sort((a, b) => a.cellId - b.cellId)) {
    const list = byTeam.get(p.team) ?? [];
    list.push(p);
    byTeam.set(p.team, list);
  }
  const indices = new Map<number, number>();
  for (const members of byTeam.values()) {
    members.forEach((m, i) => indices.set(m.cellId, i + 1));
  }
  return indices;
}
