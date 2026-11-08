package net.jemsit.auth.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code ddl-auto=update} adds columns but never widens them, and {@code tokens} was created with
 * varchar(255) - too short once tokens carry an id and long usernames. Widening is a no-op once
 * done, so this simply runs on every start.
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class WidenTokenColumns implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WidenTokenColumns.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE tokens
                        ALTER COLUMN token TYPE varchar(1024),
                        ALTER COLUMN refresh_token TYPE varchar(1024),
                        ALTER COLUMN previous_refresh_token TYPE varchar(1024)
                    """);
        } catch (RuntimeException e) {
            log.warn("Could not widen the tokens columns: {}", e.getMessage());
        }
    }
}
