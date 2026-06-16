/**
 * Pure view-model builder for the champ-select overlay. Combines the anonymized participants, the
 * backend's pick recommendations and the win probability into a render-ready shape, resolving
 * champion ids to names. Kept DOM-free for unit testing.
 */
import type { PickRecommendations, Role, WinProbability } from "../backend/types";

/** Anonymized participant as produced by rankedAnonymizer (identity fields may be stripped). */
export interface OverlayParticipant {
  championId?: number;
  team: number;
  displayName?: string;
}

export interface DraftOverlayInput {
  participants: OverlayParticipant[];
  recommendations: PickRecommendations | null;
  winProbability: WinProbability | null;
  championNames: Record<number, string>;
}

export interface DraftPickView {
  name: string;
  score: number;
  reasons: string[];
}

export interface DraftOverlayModel {
  role: Role | null;
  winProbabilityPct: number | null;
  winProbabilitySource: string | null;
  allies: string[];
  enemies: string[];
  topPicks: DraftPickView[];
}

function championName(id: number | undefined, names: Record<number, string>): string {
  if (!id || id <= 0) {
    return "Picking…";
  }
  return names[id] ?? `Champion ${id}`;
}

export function buildDraftOverlayModel(input: DraftOverlayInput, topN = 5): DraftOverlayModel {
  const { participants, recommendations, winProbability, championNames } = input;

  const allies = participants
    .filter((p) => p.team === 0)
    .map((p) => championName(p.championId, championNames));
  const enemies = participants
    .filter((p) => p.team === 1)
    .map((p) => championName(p.championId, championNames));

  const topPicks: DraftPickView[] = (recommendations?.recommendations ?? [])
    .slice(0, topN)
    .map((r) => ({ name: r.name, score: r.score, reasons: r.reasons }));

  const pct =
    winProbability && typeof winProbability.winProbability === "number"
      ? Math.round(winProbability.winProbability * 100)
      : null;

  return {
    role: recommendations?.role ?? null,
    winProbabilityPct: pct,
    winProbabilitySource: winProbability?.source ?? null,
    allies,
    enemies,
    topPicks,
  };
}
