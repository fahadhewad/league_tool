/** Client for the LeagueTool Spring backend. */
import type {
  AnalyzeRequest,
  PickRecommendations,
  RecommendRequest,
  WinProbability,
} from "./types";

type FetchLike = (input: string, init?: RequestInit) => Promise<Response>;

export interface BackendClientOptions {
  baseUrl?: string;
  fetchImpl?: FetchLike;
}

export class BackendClient {
  private readonly baseUrl: string;
  private readonly fetchImpl: FetchLike;

  constructor(options: BackendClientOptions = {}) {
    this.baseUrl = options.baseUrl ?? "http://localhost:8080";
    this.fetchImpl = options.fetchImpl ?? fetch;
  }

  getProfile(platform: string, gameName: string, tagLine: string, matchCount?: number): Promise<unknown> {
    const query = matchCount !== undefined ? `?matchCount=${matchCount}` : "";
    const path = `/api/v1/profiles/${platform}/${encodeURIComponent(gameName)}/${encodeURIComponent(tagLine)}${query}`;
    return this.json(path);
  }

  recommend(request: RecommendRequest): Promise<PickRecommendations> {
    return this.json<PickRecommendations>("/api/v1/draft/recommend", this.post(request));
  }

  analyze(request: AnalyzeRequest): Promise<unknown> {
    return this.json("/api/v1/draft/analyze", this.post(request));
  }

  winProbability(request: AnalyzeRequest): Promise<WinProbability> {
    return this.json<WinProbability>("/api/v1/draft/win-probability", this.post(request));
  }

  private post(body: unknown): RequestInit {
    return { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) };
  }

  private async json<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await this.fetchImpl(this.baseUrl + path, init);
    if (!response.ok) {
      throw new Error(`Backend ${path} returned ${response.status}`);
    }
    return (await response.json()) as T;
  }
}
