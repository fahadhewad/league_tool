package gg.leaguetool.draft.winprob;

import com.github.tomakehurst.wiremock.WireMockServer;
import gg.leaguetool.draft.model.WinProbability;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class WinProbabilityClientTest {

    private WireMockServer server;
    private WinProbabilityClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        // Force HTTP/1.1 (as production does) so WireMock's HTTP/2 support isn't negotiated.
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + server.port())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
        client = new WinProbabilityClient(restClient);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void mapsModelResponse() {
        server.stubFor(post(urlEqualTo("/predict")).willReturn(
                okJson("{\"win_probability\":0.58,\"model_loaded\":true,\"note\":null}")));

        WinProbability result = client.predict(List.of(86, 64), List.of(122));

        assertThat(result.winProbability()).isEqualTo(0.58);
        assertThat(result.modelLoaded()).isTrue();
        assertThat(result.source()).isEqualTo("ml-model");
    }

    @Test
    void usesFallbackWhenModelNotLoaded() {
        server.stubFor(post(urlEqualTo("/predict")).willReturn(
                okJson("{\"win_probability\":0.5,\"model_loaded\":false,\"note\":\"no model\"}")));

        WinProbability result = client.predict(List.of(86), List.of(122));

        assertThat(result.modelLoaded()).isFalse();
        assertThat(result.source()).isEqualTo("ml-fallback");
    }

    @Test
    void degradesGracefullyOnServerError() {
        server.stubFor(post(urlEqualTo("/predict")).willReturn(aResponse().withStatus(500)));

        WinProbability result = client.predict(List.of(86), List.of(122));

        assertThat(result.source()).isEqualTo("unavailable");
        assertThat(result.winProbability()).isEqualTo(0.5);
    }
}
