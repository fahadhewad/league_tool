package gg.leaguetool.common.security;

import gg.leaguetool.config.AdminSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthFilterTest {

    private static final AdminSecurityProperties ENABLED =
            new AdminSecurityProperties("s3cret", "X-Admin-Token");
    private static final AdminSecurityProperties DISABLED =
            new AdminSecurityProperties("", "X-Admin-Token");

    @Test
    void allowsAdminRequestWithCorrectToken() throws Exception {
        MockFilterChain chain = run(ENABLED, "/api/v1/admin/crawl", "s3cret");
        assertThat(chain.getRequest()).as("chain proceeded").isNotNull();
    }

    @Test
    void rejectsAdminRequestWithWrongToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(ENABLED, "/api/v1/admin/crawl", "nope", response);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).as("chain blocked").isNull();
    }

    @Test
    void rejectsAdminRequestWithNoToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(ENABLED, "/api/v1/admin/crawl", null, response);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void failsClosedWhenNoTokenConfigured() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(DISABLED, "/api/v1/admin/crawl", "anything", response);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void ignoresNonAdminPaths() throws Exception {
        // Public endpoints must pass straight through even with no token configured.
        MockFilterChain chain = run(DISABLED, "/api/v1/champions", null);
        assertThat(chain.getRequest()).as("public request proceeded").isNotNull();
    }

    private static MockFilterChain run(AdminSecurityProperties props, String uri, String token)
            throws Exception {
        return run(props, uri, token, new MockHttpServletResponse());
    }

    private static MockFilterChain run(AdminSecurityProperties props, String uri, String token,
                                       MockHttpServletResponse response) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        if (token != null) {
            request.addHeader("X-Admin-Token", token);
        }
        MockFilterChain chain = new MockFilterChain();
        new AdminAuthFilter(props).doFilter(request, response, chain);
        return chain;
    }
}
