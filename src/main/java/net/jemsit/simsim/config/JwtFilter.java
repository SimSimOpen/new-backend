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
