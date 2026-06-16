/**
 * Thin read-only client for the LCU REST API. Used to read the champ-select session and the local
 * summoner. The LCU uses a self-signed certificate, so the caller supplies a fetch implementation
 * configured to accept it (in Electron's main process via an https agent).
 */
import { lcuAuthHeader, lcuBaseUrl, type Lockfile } from "./lockfile";

export interface LcuChampSelectCell {
  cellId: number;
  championId: number;
  championPickIntentId?: number;
  assignedPosition?: string;
  puuid?: string;
  summonerId?: number;
  team?: number;
}

export interface LcuChampSelectSession {
  localPlayerCellId: number;
  myTeam: LcuChampSelectCell[];
  theirTeam: LcuChampSelectCell[];
  actions?: Array<Array<{ type: string; championId: number; completed: boolean }>>;
  bans?: { myTeamBans: number[]; theirTeamBans: number[] };
}

export interface LcuSummoner {
  summonerId: number;
  puuid: string;
  gameName: string;
  tagLine: string;
  summonerLevel: number;
}

type FetchLike = (input: string, init?: RequestInit) => Promise<Response>;

export class LcuClient {
  private readonly baseUrl: string;
  private readonly auth: string;
  private readonly fetchImpl: FetchLike;

  constructor(lockfile: Lockfile, fetchImpl: FetchLike) {
    this.baseUrl = lcuBaseUrl(lockfile);
    this.auth = lcuAuthHeader(lockfile);
    this.fetchImpl = fetchImpl;
  }

  async getChampSelectSession(): Promise<LcuChampSelectSession> {
    return this.get<LcuChampSelectSession>("/lol-champ-select/v1/session");
  }

  async getCurrentSummoner(): Promise<LcuSummoner> {
    return this.get<LcuSummoner>("/lol-summoner/v1/current-summoner");
  }

  /** Current gameflow queue id (e.g. 420 = ranked solo), or undefined if unavailable. */
  async getQueueId(): Promise<number | undefined> {
    try {
      const session = await this.get<{ gameData?: { queue?: { id?: number } } }>(
        "/lol-gameflow/v1/session",
      );
      return session.gameData?.queue?.id;
    } catch {
      return undefined;
    }
  }

  private async get<T>(path: string): Promise<T> {
    const response = await this.fetchImpl(this.baseUrl + path, {
      headers: { Authorization: this.auth, Accept: "application/json" },
    });
    if (!response.ok) {
      throw new Error(`LCU ${path} returned ${response.status}`);
    }
    return (await response.json()) as T;
  }
}
