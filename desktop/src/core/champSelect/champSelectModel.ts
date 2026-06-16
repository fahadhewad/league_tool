/** Maps an LCU champ-select session into the backend's draft model. */
import type { LcuChampSelectSession } from "../lcu/lcuClient";
import type { DraftPick, Role } from "../backend/types";

export interface ChampSelectRecommendInput {
  role: Role | null;
  allies: DraftPick[];
  enemies: DraftPick[];
  bans: number[];
}

export function positionToRole(position?: string): Role | null {
  switch ((position ?? "").toLowerCase()) {
    case "top":
      return "TOP";
    case "jungle":
      return "JUNGLE";
    case "middle":
      return "MIDDLE";
    case "bottom":
      return "BOTTOM";
    case "utility":
      return "UTILITY";
    default:
      return null;
  }
}

/** Build a recommend/analyze input from the session, excluding the local player from "allies". */
export function toRecommendInput(session: LcuChampSelectSession): ChampSelectRecommendInput {
  const me = session.myTeam.find((c) => c.cellId === session.localPlayerCellId);
  const role = positionToRole(me?.assignedPosition);

  const allies: DraftPick[] = session.myTeam
    .filter((c) => c.cellId !== session.localPlayerCellId && c.championId > 0)
    .map((c) => ({ championId: c.championId, role: positionToRole(c.assignedPosition) }));

  const enemies: DraftPick[] = session.theirTeam
    .filter((c) => c.championId > 0)
    .map((c) => ({ championId: c.championId, role: positionToRole(c.assignedPosition) }));

  return { role, allies, enemies, bans: collectBans(session) };
}

function collectBans(session: LcuChampSelectSession): number[] {
  const bans: number[] = [];
  if (session.bans) {
    bans.push(...(session.bans.myTeamBans ?? []), ...(session.bans.theirTeamBans ?? []));
  } else if (session.actions) {
    for (const phase of session.actions) {
      for (const action of phase) {
        if (action.type === "ban" && action.completed && action.championId > 0) {
          bans.push(action.championId);
        }
      }
    }
  }
  return bans.filter((id) => id > 0);
}
