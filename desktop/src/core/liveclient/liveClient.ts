/**
 * Client for Riot's Live Client Data API (https://127.0.0.1:2999/liveclientdata/*), available while
 * in an active game. Read-only HTTP polling — no memory access. The endpoint uses a self-signed
 * certificate, so a fetch implementation that accepts it is injected.
 */
export interface LiveGameStats {
  gameMode: string;
  gameTime: number;
}

export interface LivePlayer {
  championName: string;
  summonerName: string;
  team: string;
  position?: string;
  scores?: { kills: number; deaths: number; assists: number; creepScore: number };
}

export interface LiveEvent {
  EventID: number;
  EventName: string;
  EventTime: number;
}

type FetchLike = (input: string, init?: RequestInit) => Promise<Response>;

export class LiveClient {
  private readonly baseUrl: string;
  private readonly fetchImpl: FetchLike;

  constructor(fetchImpl: FetchLike, baseUrl = "https://127.0.0.1:2999") {
    this.fetchImpl = fetchImpl;
    this.baseUrl = baseUrl;
  }

  allGameData(): Promise<unknown> {
    return this.get("/liveclientdata/allgamedata");
  }

  gameStats(): Promise<LiveGameStats> {
    return this.get<LiveGameStats>("/liveclientdata/gamestats");
  }

  playerList(): Promise<LivePlayer[]> {
    return this.get<LivePlayer[]>("/liveclientdata/playerlist");
  }

  async events(): Promise<LiveEvent[]> {
    const data = await this.get<{ Events: LiveEvent[] }>("/liveclientdata/eventdata");
    return data.Events ?? [];
  }

  private async get<T>(path: string): Promise<T> {
    const response = await this.fetchImpl(this.baseUrl + path, {
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      throw new Error(`Live Client ${path} returned ${response.status}`);
    }
    return (await response.json()) as T;
  }
}
