/** Shapes mirroring the Spring backend API. */

export type Role = "TOP" | "JUNGLE" | "MIDDLE" | "BOTTOM" | "UTILITY";

export interface DraftPick {
  championId: number;
  role: Role | null;
}

export interface AnalyzeRequest {
  allies: DraftPick[];
  enemies: DraftPick[];
}

export interface RecommendRequest {
  role: Role;
  allies: DraftPick[];
  enemies: DraftPick[];
  bans: number[];
}

export interface PickRecommendation {
  championId: number;
  name: string;
  role: Role;
  score: number;
  synergyScore: number;
  counterScore: number;
  damageFitScore: number;
  reasons: string[];
}

export interface PickRecommendations {
  role: Role;
  recommendations: PickRecommendation[];
}

export interface WinProbability {
  winProbability: number;
  modelLoaded: boolean;
  source: string;
}
