package net.jemsit.product.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class GinIndexRunner implements ApplicationRunner {

    private static final List<String> INDEXES = List.of(
            """
            CREATE INDEX IF NOT EXISTS idx_property_search ON properties
            USING GIN(to_tsvector('simple', COALESCE(title, '') || ' ' || COALESCE(description, '')))
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_location_search ON property_locations
            USING GIN(to_tsvector('simple', COALESCE(address, '') || ' ' || COALESCE(country, '')))
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_regions_search ON regions
            USING GIN(to_tsvector('simple', COALESCE(name_ru, '') || ' ' || COALESCE(name_uz, '') || ' ' || COALESCE(name_oz, '')))
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_districts_search ON districts
            USING GIN(to_tsvector('simple', COALESCE(name_ru, '') || ' ' || COALESCE(name_uz, '') || ' ' || COALESCE(name_oz, '')))
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_villages_search ON villages
            USING GIN(to_tsvector('simple', COALESCE(name_ru, '') || ' ' || COALESCE(name_uz, '') || ' ' || COALESCE(name_oz, '')))
            """
    );

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        for (String statement : INDEXES) {
            try {
                jdbc.execute(statement);
            } catch (DataAccessException e) {
                log.warn("Search index not created: {}", e.getMessage());
            }
        }
    }
}
