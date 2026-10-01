package net.jemsit.simsim.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.jemsit.auth.service.JwtService;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.exceptions.TokenInvalidException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthApi authApi;

    /**
     * Read from the same property the notify module reads, so the prefix is defined once rather than
     * repeated as a literal here and in SecurityConfig.
     *
     * <p>Non-final deliberately: {@code @RequiredArgsConstructor} picks up final fields only, and
     * {@code auth/config/CreateAdminUser} already mixes {@code @Value} fields with that annotation.
     */
    @Value("${notify.base-path:/notify}")
    private String notifyBasePath;

    /**
     * The notify endpoints authenticate with their own shared secret, and the phone presents it in
     * exactly the form this filter reads a JWT: {@code Authorization: Bearer <token>}. Without this,
     * {@code extractUsername} throws {@link TokenInvalidException} and the request is answered 403
     * before it reaches {@code NotifyTokenInterceptor} - which is the only thing that should be
     * deciding.
     *
     * <p>Returning true skips {@link #doFilterInternal} entirely, so neither the SecurityContext nor
     * the UserContext is populated for these requests. That is correct: the notify module has no
     * concept of a user and nothing it calls reads UserContext.
     *
     * <p>This covers both registrations of this filter - it is a {@code @Component}, so Boot also
     * registers it in the servlet chain, and SecurityConfig adds it inside the security chain. The
     * default is {@code false}, so for every path outside the notify prefix the behaviour is
     * unchanged.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String base = notifyBasePath == null || notifyBasePath.isBlank() ? "/notify" : notifyBasePath.trim();
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
        return path.equals(base) || path.startsWith(base + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String jwt = header.substring(7);
            try {
                String username = jwtService.extractUsername(jwt);
                List<Roles> roles = jwtService.extractRoles(jwt);
                Long userId = jwtService.extractUserId(jwt);
                if (userId == null) {
                    userId = authApi.getByUsername(username).id();
                }
                UserContext.userRole(roles);
                UserContext.setUserId(userId);
                UserContext.setUserToken(jwt);
                var authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        roles.stream().map(role -> new SimpleGrantedAuthority(role.name())).toList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (TokenInvalidException e) {
                UserContext.clear();
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                return;
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
