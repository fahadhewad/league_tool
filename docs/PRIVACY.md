# Privacy Policy (draft)

_Last updated: 2026-06-16. This is a developer draft to satisfy Riot's production-key requirement;
have it reviewed before public launch._

LeagueTool ("the app") is a League of Legends companion. This policy explains what data it handles
and how.

## What we process
- **Riot account data you look up**: when you search a Riot ID (or view your own/party profiles), we
  call the Riot API for that account's PUUID, summoner profile, ranked stats, recent match history,
  and champion mastery, and display it to you.
- **Champ-select / in-game state** (desktop app only): read locally from the official **LCU** and
  **Live Client Data** APIs to power draft advice and the overlay. This stays on your machine except
  for the champion ids sent to our backend to compute recommendations.
- **Crawled match data** (backend): public ranked match data retrieved via the Riot Match-V5 API is
  stored in aggregate (champion win-rate / synergy / counter counts) to train and serve the model.

## What we do NOT do
- We do **not** read game memory or inject into the client. Only official local APIs are used.
- We do **not** de-anonymize ranked champ select. In ranked Solo/Flex, non-party participants are
  shown only as `Ally #n` / `Enemy #n`, and their identifiers are discarded before reaching the UI.
- We do **not** sell personal data or use it for advertising.

## Storage & retention
- The Riot API key is supplied via environment variable and never stored in the repository.
- Profile/match responses are cached briefly to respect Riot rate limits (minutes to days by type).
- Aggregated match statistics contain champion-level counts, not personal profiles.

## Third parties
- **Riot Games API** — all account, match, and live-game data originates here, subject to Riot's
  terms. Static assets come from **Data Dragon / Community Dragon**.

## Your choices
- Profiles are only fetched for Riot IDs you explicitly search or your own party.
- You can run the entire stack locally; no account is required to use the app.

## Compliance
This app is an independent project, **not endorsed by or affiliated with Riot Games**, and adheres to
Riot's Developer and General policies. See [`COMPLIANCE.md`](COMPLIANCE.md) for the engineering gates.

## Contact
For privacy questions, contact the maintainer via the repository's issues page.
