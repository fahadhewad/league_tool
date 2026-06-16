package gg.leaguetool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the LeagueTool backend.
 *
 * <p>A compliance-first League of Legends companion API: player profiles, recent-form scoring,
 * draft analysis, a champion pick recommender, and a gateway to the Riot API with rate limiting
 * and caching.
 */
@SpringBootApplication
@EnableCaching
public class LeagueToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeagueToolApplication.class, args);
    }
}
