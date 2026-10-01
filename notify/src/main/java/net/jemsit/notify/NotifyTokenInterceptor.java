package net.jemsit.notify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Enforces {@code notify.token} on every endpoint this module exposes.
 *
 * <p>An interceptor rather than a check inside each controller method, so that adding an endpoint
 * later cannot accidentally ship without it. That is the failure mode worth designing against here:
 * the endpoints are unauthenticated by default, and one new mapping that forgets the check is a hole
 * nobody notices until someone else finds it.
 *
 * <p>Does nothing at all when no token is configured, which keeps the module zero-config on a LAN.
 */
@Slf4j
public class NotifyTokenInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";

    private final byte[] expected;

    NotifyTokenInterceptor(NotifyProperties properties) {
        String configured = properties.getToken();
        this.expected = configured == null || configured.isBlank()
                ? null
                : configured.trim().getBytes(StandardCharsets.UTF_8);
        if (this.expected == null) {
            log.warn("notify.token is not set - every {} endpoint is reachable without credentials."
                            + " Fine on a LAN, not fine on a public host: anyone could publish"
                            + " notifications to your phone or read the ones it already produced.",
                    properties.getBasePath());
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (expected == null) {
            return true;
        }
        if (matches(request)) {
            return true;
        }
        // Logged without the presented value. A wrong token is worth knowing about; copying it into
        // the host's log is how a secret that was merely mistyped ends up in a log aggregator.
        log.warn("rejected unauthenticated notify request {} {}", request.getMethod(),
                request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"invalid or missing notify token\"}");
        return false;
    }

    private boolean matches(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)
                && fixedEquals(header.substring(BEARER.length()).trim())) {
            return true;
        }
        // The query parameter exists because an SSE connection is often opened by a client that cannot
        // set headers, and because curl-ing the stream by hand during setup should not require -H.
        return fixedEquals(request.getParameter("token"));
    }

    /**
     * Constant-time comparison. A plain {@code equals} short-circuits on the first differing byte,
     * which leaks the token's length and prefix through response timing. That is a narrow attack on a
     * shared secret over a network with milliseconds of jitter, but the correct comparison costs
     * nothing extra here.
     */
    private boolean fixedEquals(String presented) {
        if (presented == null) {
            return false;
        }
        return MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), expected);
    }
}
