package gg.leaguetool;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifies the Spring context wires together (no Riot key required to boot). */
@SpringBootTest(properties = "riot.api.key=")
class LeagueToolApplicationTests {

    @Test
    void contextLoads() {
    }
}
