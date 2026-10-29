package net.jemsit.simsim.config;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${notify.base-path:/notify}") String notifyBasePath) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(
                                "/api/auth/v1/authenticate",
                                "/api/auth/v1/firebase-login",
                                "/api/event/send-otp",
                                "/api/event/send-otp-telegram",
                                "/api/event/verify-otp",
                                // Presence heartbeats come from anonymous website visitors too.
                                "/api/event/presence/heartbeat",
                                "/api/auth/v1/register/**",
                                "/api/profile/v1/create",
                                "/api/product/v1/property/all/published",
                                "/api/product/v1/property/filter",
                                "/api/product/v1/property/*/views",
                                "/api/product/v1/car/filter",
                                "/api/product/v1/car/*/views",
                                "/api/product/v1/second-hand/filter",
                                "/api/product/v1/second-hand/*/views",
                                "/api/product/v1/services/filter",
                                "/api/product/v1/services/*/views",
                                "/actuator/health",
                                "/error",
                                // The notify module guards its own endpoints with NotifyTokenInterceptor
                                // using a shared secret. Security has to stand aside or its 401 entry
                                // point fires first and the phone never reaches the interceptor. Built
                                // from the same property JwtFilter skips on, so the module, this list
                                // and that filter cannot drift apart.
                                notifyBasePath + "/**"
                        ).permitAll()
                        // Read-only: the client's home page lists categories anonymously, but
                        // updating one (PUT on the same path) still requires an admin.
                        .requestMatchers(HttpMethod.GET, "/api/product/v1/category/**").permitAll()
                        // Read-only lookup lists (regions/districts/places) for the public browse filter.
                        .requestMatchers(HttpMethod.GET, "/api/product/v1/location/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}