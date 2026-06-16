package gg.leaguetool.champion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import gg.leaguetool.config.DataDragonProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class DataDragonServiceTest {

    private WireMockServer server;
    private DataDragonService service;
    private ChampionRepository repository;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        server.stubFor(get(urlEqualTo("/api/versions.json")).willReturn(okJson(fixture("versions.json"))));
        server.stubFor(get(urlEqualTo("/cdn/16.12.1/data/en_US/champion.json"))
                .willReturn(okJson(fixture("champion.json"))));

        DataDragonProperties props = new DataDragonProperties(
                "http://localhost:" + server.port(), "en_US", false, 8000);
        RestClient restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
        DataDragonClient client = new DataDragonClient(restClient, props);

        ObjectMapper mapper = new ObjectMapper();
        repository = new ChampionRepository(mapper);
        service = new DataDragonService(client, repository, props, mapper);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void refreshLoadsTheRosterAndRecordsTheVersion() {
        int count = service.refresh();

        assertThat(count).isEqualTo(4);
        assertThat(service.currentVersion()).isEqualTo("16.12.1");
        assertThat(repository.all()).hasSize(4);
    }

    @Test
    void appliesCuratedOverridesForKnownChampions() {
        service.refresh();

        Champion garen = repository.byId(86).orElseThrow();
        assertThat(garen.name()).isEqualTo("Garen");
        assertThat(garen.roles()).containsExactly(Role.TOP);
        assertThat(garen.damageType()).isEqualTo(DamageType.PHYSICAL);
        assertThat(garen.cc()).isFalse();
        assertThat(garen.tags()).containsExactly(ChampionTag.FIGHTER, ChampionTag.TANK);

        Champion leona = repository.byId(89).orElseThrow();
        assertThat(leona.roles()).containsExactly(Role.UTILITY);
        assertThat(leona.cc()).isTrue();
    }

    @Test
    void derivesAttributesForChampionsWithoutOverrides() {
        service.refresh();

        // Smolder: Marksman+Mage so damage falls through to attack(2) vs magic(6) -> MAGIC; marksman -> BOTTOM
        Champion smolder = repository.byId(901).orElseThrow();
        assertThat(smolder.name()).isEqualTo("Smolder");
        assertThat(smolder.roles()).containsExactly(Role.BOTTOM);
        assertThat(smolder.damageType()).isEqualTo(DamageType.MAGIC);
        assertThat(smolder.cc()).isFalse();

        // Naafiri: Assassin -> MIDDLE; attack(8) > magic(2) -> PHYSICAL
        Champion naafiri = repository.byId(950).orElseThrow();
        assertThat(naafiri.roles()).containsExactly(Role.MIDDLE);
        assertThat(naafiri.damageType()).isEqualTo(DamageType.PHYSICAL);
    }

    private static String fixture(String name) {
        try (InputStream in = DataDragonServiceTest.class.getResourceAsStream("/fixtures/ddragon/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
