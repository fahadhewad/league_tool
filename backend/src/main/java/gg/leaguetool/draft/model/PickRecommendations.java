package gg.leaguetool.draft.model;

import gg.leaguetool.champion.Role;

import java.util.List;

/** Ranked champion recommendations for an open role. */
public record PickRecommendations(Role role, List<PickRecommendation> recommendations) {
}
