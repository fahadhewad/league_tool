package gg.leaguetool.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient for the Python ML service. Short timeouts keep champ select responsive even if the ML
 * service is slow or down — the win-probability call degrades to a neutral fallback in that case.
 */
@Configuration
public class MlServiceConfig {

    @Bean
    RestClient mlRestClient(@Value("${ml.service.url:http://localhost:8001}") String baseUrl,
                            RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(1500));
        factory.setReadTimeout(Duration.ofMillis(3000));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
}
