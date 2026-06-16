# Compliance — Riot policy & anti-cheat (hard gates)

These are **non-negotiable** design constraints. Features that would violate them are explicitly
out of scope and must never be added.

## Hard gates
1. **Ranked anonymity.** In ranked Solo/Flex champ select, all non-party participant identities are
   obfuscated to `Ally #1…` / `Enemy #1…`. We never resolve, display, or look up names/stats of
   random teammates or opponents in ranked. (Normal/Draft modes show names in-client, so scouting
   there is allowed.)
2. **No "dodge this teammate."** Any feature that recommends dodging based on a random ranked
   teammate's identity is forbidden — it requires the de-anonymization above.
3. **No memory reading / no injection.** We only use official local APIs: the LCU (lockfile + REST/WS)
   and the Live Client Data API (`127.0.0.1:2999`). We never read game memory or inject into the client.
   This keeps the tool clear of anti-cheat (e.g. Vanguard).
4. **Rate limits.** Never exceed Riot API rate limits. A request queue + backoff + caching enforce this.
5. **Privacy & ToS.** A privacy policy and adherence to Riot's General Policies are required before
   requesting a production key.

## Why the anonymity gate matters
Riot intentionally anonymizes ranked champ select. Defeating that anonymization (by matching
spectator data, lobby timing, or any side channel) is exactly what anti-cheat systems are built to
catch. The tool's value comes from analyzing **your own** picks and the **visible** information —
not from revealing what Riot chose to hide.

## How each surface stays compliant
| Surface | Compliant behavior |
|---|---|
| Profile lookup | Only for Riot IDs the user explicitly searches or their own party. |
| Recent-form score | Same — explicit ID or party member. |
| Draft analyzer / pick recommender | Operates on champions (not identities); fully allowed. |
| Champ-select overlay (ranked) | Shows your picks + anonymized ally/enemy slots only. |
| Champ-select overlay (normal/draft) | May show names (visible in-client). |
| In-game overlay | Live Client Data API only; no memory, no input interference. |

## Engineering guardrails (enforced in code)
- A `RankedAnonymizer` component sits between the LCU champ-select session and the UI; in
  Solo/Flex queues it strips/obfuscates all non-party summoner identities before they reach the renderer.
- The backend refuses to resolve identities for participants flagged as anonymized.
- No native modules that touch process memory are permitted in `desktop/`.

## Acceptance criteria (compliance)
- [ ] Ranked Solo/Flex champ select: all non-party names obfuscated.
- [ ] No memory reading / injection anywhere in the codebase.
- [ ] Privacy policy published; Riot General Policies adhered to before production-key request.
- [ ] Zero `429`-driven outages (rate-limit queue verified under load).
