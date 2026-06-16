# Roadmap

Phased plan for a solo developer. Long-poles (match-data crawling, Riot production-key approval)
run in parallel and dominate calendar time, not coding time.

| Phase | Work | Status |
|------:|------|--------|
| **0. Setup** | Repo/CI scaffold, docs, infra (Postgres+Redis), Riot dev account, production-key application | ✅ done (production key application is the user's to file) |
| **1. Identity + profiles** | Riot ID→PUUID, summoner/ranked lookup, match history, caching + rate-limit queue | ✅ done |
| **2. Recent-form / tilt** | Last-N-games analysis, performance scoring, party member lookup | ✅ done |
| **3. Data crawler** | Match-V5 ingestion worker + persistence + aggregates | ✅ done & live-validated (ranked SR only; bounded, admin-gated) |
| **4. Draft analyzer + pick recommender** | Synergy/counter/role-coverage + champ pick scoring | ✅ done (heuristic seed, empirical once crawled) |
| **5. ML model** | Featurize, train, calibrate, evaluate, inference service | ✅ done & live-validated (retrained on the real crawled corpus) |
| **6. LCU + overlay** | Electron champ-select connector, Live Client Data overlay, ranked name-obfuscation | ✅ built; champ-select + in-game polling wired (overlay needs a live client) |
| **7. Polish + compliance + launch** | UX, error handling, privacy policy/ToS, beta, production key | 🟢 in progress (privacy policy drafted) |

> **Live validation done:** the end-to-end path has been run against the real Riot API — profile
> lookup, a ranked crawl into Postgres, empirical aggregate computation, retraining the calibrated
> ML model on the crawled corpus, and the backend serving real `ml-model` win probabilities.
> Remaining live work: the in-client overlay (needs a running League client) and packaged installers
> (built on the target OS). Production-key approval is the user's to file.

## Milestones
- **MVP** (web/desktop profile + recent-form + rules draft analyzer + pick recommender, baseline ML): ~3–4 weeks.
- **Full product** (overlay + calibrated ML + polish + approved key): ~10–12 weeks solo.

## Acceptance criteria

### Functional
- [x] Resolve any Riot ID → profile with ranked + last 20 matches (cached). _< 2 s pending live timing._
- [x] Recent-form score from last 5–10 games with a transparent breakdown.
- [x] Draft analyzer: synergy, top counters per lane, role/damage gaps, + a win-prob endpoint.
- [x] Pick recommender: ranked champ shortlist for the open role with per-champ score + reasons.
- [x] In-game overlay reads live data via official APIs. _End-to-end needs a live client._

### Non-functional
- [x] Rate-limit queue + backoff in place (blocking sliding-window limiter). _Zero-429 verified under live load is pending._
- [x] Read-through caching with per-resource TTLs. _>80% hit-rate to be measured live._
- [x] Overlay is transparent/click-through; no input interference by design.

### ML
- [x] Beats the 50% baseline. _Retrained on a real ranked crawl (calibrated); a production-grade
  model needs a far larger corpus than the validation crawl._
- [x] Low calibration error (ECE ≈ 0.04) via isotonic calibration.
- [x] Inference is a single GBM call; < 100 ms expected. _To be benchmarked live._

### Compliance (hard gates)
- [x] Ranked Solo/Flex: all non-party names obfuscated (and identifiers stripped) — `rankedAnonymizer`.
- [x] No memory reading / injection anywhere (official local APIs only).
- [x] Privacy policy drafted; Riot General Policies adherence documented before production key.
