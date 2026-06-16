package gg.leaguetool.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Builds the {@link RestClient} used to call the Riot API.
 *
 * <p>The Riot API key is attached to every request via an interceptor, so individual call sites
 * never deal with auth. Timeouts come from {@link RiotApiProperties}.
 */
@Configuration
@EnableConfigurationProperties(RiotApiProperties.class)
public class RestClientConfig {

    public static final String RIOT_TOKEN_HEADER = "X-Riot-Token";

    @Bean
    ClientHttpRequestFactory riotRequestFactory(RiotApiProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.timeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.timeoutMs()));
        return factory;
    }

    @Bean
    RestClient riotRestClient(RiotApiProperties props,
                              ClientHttpRequestFactory riotRequestFactory,
                              RestClient.Builder builder) {
        // Use Boot's autoconfigured builder so the JSON converter ignores the many Riot fields
        // we don't model (FAIL_ON_UNKNOWN_PROPERTIES is disabled by Spring Boot).
        return builder
                .requestFactory(riotRequestFactory)
                .requestInterceptor((request, body, execution) -> {
                    if (props.hasKey()) {
                        request.getHeaders().set(RIOT_TOKEN_HEADER, props.key());
                    }
                    request.getHeaders().set("Accept", "application/json");
                    return execution.execute(request, body);
                })
                .build();
    }
}
