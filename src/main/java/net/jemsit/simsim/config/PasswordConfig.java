package net.jemsit.simsim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    // kept out of SecurityConfig: AuthServiceImpl needs this encoder, and SecurityConfig needs
    // JwtFilter, which needs AuthApi -> AuthService, so defining it there closes a bean cycle
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
