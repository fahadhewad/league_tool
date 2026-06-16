package gg.leaguetool.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI / Swagger UI metadata, served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI leagueToolOpenApi() {
        return new OpenAPI().info(new Info()
                .title("LeagueTool API")
                .version("0.1.0")
                .description("Compliance-first League of Legends companion: profiles, recent-form, "
                        + "draft analysis, and a champion pick recommender.")
                .license(new License().name("MIT")));
    }
}
