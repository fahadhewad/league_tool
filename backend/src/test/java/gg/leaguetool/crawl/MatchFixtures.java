package gg.leaguetool.crawl;

import gg.leaguetool.riot.dto.MatchDto;

import java.util.List;

/** Builders for synthetic Match-V5 payloads used in crawl/ingest tests. */
public final class MatchFixtures {

    private static final String[] POSITIONS = {"TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY"};

    private MatchFixtures() {
    }

    public static MatchDto.Participant participant(String puuid, int championId, int teamId, String position, boolean win) {
        return new MatchDto.Participant(puuid, "Name", "TAG", championId, "Champ" + championId,
                position, position, teamId, win, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static MatchDto match(String matchId, int queueId, List<MatchDto.Participant> participants) {
        List<String> puuids = participants.stream().map(MatchDto.Participant::puuid).toList();
        return new MatchDto(
                new MatchDto.Metadata("2", matchId, puuids),
                new MatchDto.Info(0, 1800, 0, 0, queueId, "14.23.1", "CLASSIC", "MATCHED_GAME", participants));
    }

    /**
     * A standard 10-player Summoner's Rift match. Team 100 has champions {@code base..base+4} (one per
     * role), team 200 has {@code base+5..base+9}; {@code blueWins} decides the result.
     */
    public static MatchDto standardMatch(String matchId, int base, boolean blueWins) {
        return match(matchId, 420, List.of(
                participant("p" + base + "-0", base, 100, POSITIONS[0], blueWins),
                participant("p" + base + "-1", base + 1, 100, POSITIONS[1], blueWins),
                participant("p" + base + "-2", base + 2, 100, POSITIONS[2], blueWins),
                participant("p" + base + "-3", base + 3, 100, POSITIONS[3], blueWins),
                participant("p" + base + "-4", base + 4, 100, POSITIONS[4], blueWins),
                participant("p" + base + "-5", base + 5, 200, POSITIONS[0], !blueWins),
                participant("p" + base + "-6", base + 6, 200, POSITIONS[1], !blueWins),
                participant("p" + base + "-7", base + 7, 200, POSITIONS[2], !blueWins),
                participant("p" + base + "-8", base + 8, 200, POSITIONS[3], !blueWins),
                participant("p" + base + "-9", base + 9, 200, POSITIONS[4], !blueWins)));
    }
}
