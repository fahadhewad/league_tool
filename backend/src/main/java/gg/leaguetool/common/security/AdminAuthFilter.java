package gg.leaguetool.common.security;

import gg.leaguetool.config.AdminSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Guards operator endpoints under {@code /api/v1/admin/**} with a shared-secret header.
 *
 * <p>Fails closed: if no token is configured the admin surface is disabled entirely (503), so a
 * fresh deployment cannot be driven by an anonymous caller. When configured, a request must carry
 * the exact token (compared in constant time) or it is rejected with 401.
 */
public class AdminAuthFilter extends OncePerRequestFilter {

    static final String ADMIN_PATH_PREFIX = "/api/v1/admin/";

    private final AdminSecurityProperties props;

    public AdminAuthFilter(AdminSecurityProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!props.enabled()) {
            deny(response, HttpStatus.SERVICE_UNAVAILABLE,
                    "Admin endpoints are disabled. Set ADMIN_API_TOKEN to enable them.");
            return;
        }
        String presented = request.getHeader(props.headerName());
        if (presented == null || !constantTimeEquals(presented, props.apiToken())) {
            deny(response, HttpStatus.UNAUTHORIZED, "Missing or invalid admin token.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static void deny(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                        + "\",\"message\":\"" + message + "\"}");
    }
}
