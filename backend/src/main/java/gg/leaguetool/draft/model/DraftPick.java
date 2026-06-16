package gg.leaguetool.draft.model;

import gg.leaguetool.champion.Role;

/**
 * A champion locked (or hovered) in champ select, optionally with its assigned role.
 *
 * @param championId Riot champion id
 * @param role       assigned position, or {@code null} if unknown
 */
public record DraftPick(int championId, Role role) {
}
