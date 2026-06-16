# Architecture

LeagueTool is a polyglot monorepo with three deployable units plus shared data stores.

## Components

### 1. Backend API — Java 21 / Spring Boot 3 (`backend/`)
The system of record and orchestration layer. Responsibilities:

- **Riot API gateway** — a single `RiotApiClient` wraps every Riot endpoint behind a
  **rate-limited request queue** (Redis token buckets) with exponential backoff on `429`
  and per-region/per-method limit tracking.
- **Caching** — Redis (shared) + Caffeine (in-process) read-through caches keyed by PUUID /
  match id / champion. Target cache-hit rate > 80% on repeat lookups.
- **Domain services**
  - `ProfileService` — Riot ID → PUUID → summoner + ranked + last-N matches + mastery.
  - `RecentFormService` — last 5–10 games → transparent performance/"tilt" breakdown.
  - `DraftAnalyzerService` — synergy, counters, role/damage coverage for a champ-select state.
  - `PickRecommenderService` — scores candidate champions for your open role given allies +
    enemies already picked (win-rate × counter × synergy), returns a ranked shortlist + reasons.
  - `WinProbabilityService` — calls the Python ML service for comp-vs-comp probability.
- **Static data** — `DataDragonService` loads & caches champion/item/spell metadata + icons.
- **Ingestion worker** — `MatchCrawler` walks the Match-V5 graph from high-elo seeds to build
  the training corpus (runs as a scheduled/bounded background job, rate-limit aware).

Build tool: **Maven** (system Maven + Maven Central are reachable in CI; no wrapper download needed).

### 2. ML service — Python 3.11 / FastAPI (`ml-service/`)
Owns the win-probability model end to end:

1. **Featurize** — champion one-hot (×2 teams) + engineered features: pairwise synergy win-rates,
   lane counter matchups, role/damage-type balance (AD/AP/CC/frontline), team mastery deltas.
2. **Train** — logistic-regression baseline → LightGBM; **calibrate** (Platt/isotonic).
3. **Serve** — `/predict` inference endpoint (target < 100 ms) the backend calls during champ select.
4. **Evaluate** — held-out accuracy, log-loss, and calibration error (ECE) reporting.

The synergy/counter tables produced here double as the data behind the rules-based draft analyzer
and pick recommender, so the work pays off twice.

### 3. Desktop client / overlay — Electron (`desktop/`)
- **LCU connector** — reads the League Client `lockfile` (port + auth token) and subscribes to the
  LCU WebSocket for champ-select session events. **Read-only; no memory access, no injection.**
- **Live Client Data API** — polls `https://127.0.0.1:2999/liveclientdata/*` for in-game state
  (timers, scoreboard, events).
- **Overlay window** — transparent, click-through where appropriate; shows draft advice and
  in-game reminders without interfering with the client.
- **Compliance layer** — in ranked Solo/Flex champ select, all non-party identities are rendered as
  `Ally #1…` / `Enemy #1…`. See [`COMPLIANCE.md`](COMPLIANCE.md).

## Data stores (`infra/`)
- **PostgreSQL** — crawled matches, computed synergy/counter aggregates, cached profiles.
- **Redis** — rate-limit token buckets + hot cache.

## Request flow: "recommend my pick"
```
Electron (champ-select state: allies[], enemies[], myRole, bans[])
   → POST /api/v1/draft/recommend  (Spring)
        → DraftAnalyzerService builds features from Postgres aggregates + DataDragon
        → PickRecommenderService scores candidate champs (rules) 
        → WinProbabilityService → ML /predict (optional ML re-rank)
   ← ranked list: [{champion, score, winRate, counterScore, synergyScore, reasons[]}]
```

## Rate-limit strategy
- Dev key ≈ 20 req/s, 100 req/2min. Production keys are higher.
- A central limiter serializes all Riot calls through Redis token buckets sized per the
  app limit **and** per-method limits returned in Riot's response headers.
- On `429`, respect `Retry-After`; otherwise exponential backoff (2s, 4s, 8s, 16s).
- Everything cacheable is cached; crawling is bounded so it never starves live traffic.

## Testing
- Backend: JUnit 5 + WireMock to stub Riot responses from committed fixtures (no live calls in CI).
- ML: pytest on synthetic/sample match frames.
- Desktop: unit tests for LCU/Live-Client parsers against captured fixtures.

## Network egress note (managed dev environments)
Some sandboxes restrict outbound egress to an allowlist. To run live, allow:
`*.api.riotgames.com`, `ddragon.leagueoflegends.com`, `*.communitydragon.org`.
Maven Central / GitHub are typically already allowed. Without egress, the stack still
builds and tests fully against committed fixtures.
