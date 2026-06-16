package gg.leaguetool.config;

import gg.leaguetool.common.security.AdminAuthFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link AdminAuthFilter} for the operator endpoints only, leaving the public API
 * (profiles, form, champions, draft) and actuator/swagger untouched.
 */
@Configuration
@EnableConfigurationProperties(AdminSecurityProperties.class)
public class AdminSecurityConfig {

    @Bean
    FilterRegistrationBean<AdminAuthFilter> adminAuthFilter(AdminSecurityProperties props) {
        FilterRegistrationBean<AdminAuthFilter> registration =
                new FilterRegistrationBean<>(new AdminAuthFilter(props));
        registration.addUrlPatterns("/api/v1/admin/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
