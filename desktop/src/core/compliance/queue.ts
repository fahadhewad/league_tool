/** Queue identification for compliance decisions. */

export const RANKED_SOLO_DUO = 420;
export const RANKED_FLEX = 440;

/**
 * Ranked Solo/Duo and Flex are the queues where Riot anonymizes champ select. In these queues we
 * must never reveal non-party participant identities. Other queues (normal/draft) show names in the
 * client, so scouting there is allowed.
 */
export function isRankedSoloOrFlex(queueId: number | undefined | null): boolean {
  return queueId === RANKED_SOLO_DUO || queueId === RANKED_FLEX;
}
