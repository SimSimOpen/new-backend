package net.jemsit.auth.config;

import net.jemsit.auth.data.repository.UserRepository;
import net.jemsit.auth.service.AuthService;
import net.jemsit.common.dto.request.auth.RegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateAdminUser implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CreateAdminUser.class);

    private final AuthService authService;
    private final UserRepository userRepository;

    @Value("${app.admin.username:admin}")
    private String username;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            log.warn("app.admin.password is not set, skipping admin user seeding");
            return;
        }
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    // Admin user already exists, do nothing
                },
                () -> authService.registerAdmin(new RegisterRequestDTO(
                        username,
                        password,
                        email,
                        "",
                        ""
                ))
        );
    }
}
