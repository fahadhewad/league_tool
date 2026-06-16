# Roadmap

Phased plan for a solo developer. Long-poles (match-data crawling, Riot production-key approval)
run in parallel and dominate calendar time, not coding time.

| Phase | Work | Status |
|------:|------|--------|
| **0. Setup** | Repo/CI scaffold, docs, infra (Postgres+Redis), Riot dev account, production-key application | 🟢 in progress |
| **1. Identity + profiles** | Riot ID→PUUID, summoner/ranked lookup, match history, caching + rate-limit queue | ⚪ planned |
| **2. Recent-form / tilt** | Last-N-games analysis, performance scoring, party member lookup | ⚪ planned |
| **3. Data crawler** | Match-V5 ingestion worker + Postgres schema (kick off early; runs for days) | ⚪ planned |
| **4. Draft analyzer + pick recommender** | Synergy/counter/role-coverage + champ pick scoring from crawled data | ⚪ planned |
| **5. ML model** | Featurize, train, calibrate, evaluate, inference service | ⚪ planned |
| **6. LCU + overlay** | Electron champ-select connector, Live Client Data overlay, ranked name-obfuscation | ⚪ planned |
| **7. Polish + compliance + launch** | UX, error handling, privacy policy/ToS, beta, production key | ⚪ planned |

## Milestones
- **MVP** (web/desktop profile + recent-form + rules draft analyzer + pick recommender, baseline ML): ~3–4 weeks.
- **Full product** (overlay + calibrated ML + polish + approved key): ~10–12 weeks solo.

## Acceptance criteria

### Functional
- [ ] Resolve any Riot ID → profile with ranked + last 20 matches in < 2 s (cached).
- [ ] Recent-form score from last 5–10 games with a transparent breakdown.
- [ ] Draft analyzer: synergy, top counters per lane, role/damage gaps, calibrated win-prob for current pick state.
- [ ] Pick recommender: ranked champ shortlist for the open role with per-champ score + reasons.
- [ ] In-game overlay shows live data without interfering with the client.

### Non-functional
- [ ] Never exceed Riot rate limits; zero `429`-driven outages.
- [ ] Cache hit rate > 80% on repeat lookups.
- [ ] Overlay adds negligible latency and no input interference.

### ML
- [ ] Beats 50% baseline on held-out test (target ≥ 55% comp-only, ≥ 58–60% with mastery features).
- [ ] Low calibration error (ECE) so the displayed % is trustworthy.
- [ ] Inference < 100 ms.

### Compliance (hard gates)
- [ ] Ranked Solo/Flex: all non-party names obfuscated.
- [ ] No memory reading / injection anywhere.
- [ ] Privacy policy + Riot General Policies adherence before production key.
