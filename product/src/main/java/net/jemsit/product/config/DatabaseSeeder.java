package net.jemsit.product.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private static final TypeReference<List<Map<String, Object>>> ROWS = new TypeReference<>() {
    };

    // Mahalla ids in data/mahallas.json are offset by this so they never collide with village ids.
    private static final long MAHALLA_ID_OFFSET = 10_000_000L;

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        if (isEmpty("regions")) {
            seedRegions();
        }
        if (isEmpty("districts")) {
            seedDistricts();
        }
        if (isEmpty("villages")) {
            seedVillages();
        }
        // Mahallas share the villages table but are seeded separately, so databases that were
        // seeded before this file existed still pick them up on the next start.
        if (!hasMahallas()) {
            seedMahallas();
        }
    }

    private boolean isEmpty(String table) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null || count == 0;
    }

    private void seedRegions() {
        for (Map<String, Object> row : readRows("data/regions.json")) {
            jdbc.update("INSERT INTO regions (id, name_uz, name_oz, name_ru) VALUES (?, ?, ?, ?)",
                    row.get("id"), row.get("name_uz"), row.get("name_oz"), row.get("name_ru"));
        }
        log.info("Seeded {} regions", countOf("regions"));
    }

    private void seedDistricts() {
        for (Map<String, Object> row : readRows("data/districts.json")) {
            jdbc.update("INSERT INTO districts (id, region_id, name_uz, name_oz, name_ru) VALUES (?, ?, ?, ?, ?)",
                    row.get("id"), row.get("region_id"), row.get("name_uz"), row.get("name_oz"), row.get("name_ru"));
        }
        log.info("Seeded {} districts", countOf("districts"));
    }

    private void seedVillages() {
        for (Map<String, Object> row : readRows("data/villages.json")) {
            jdbc.update("INSERT INTO villages (id, district_id, name_uz, name_oz, name_ru) VALUES (?, ?, ?, ?, ?)",
                    row.get("id"), row.get("district_id"), row.get("name_uz"), row.get("name_oz"), row.get("name_ru"));
        }
        log.info("Seeded {} villages", countOf("villages"));
    }

    private boolean hasMahallas() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM villages WHERE id >= ?", Integer.class, MAHALLA_ID_OFFSET);
        return count != null && count > 0;
    }

    private void seedMahallas() {
        List<Object[]> batch = readRows("data/mahallas.json").stream()
                .map(row -> new Object[]{row.get("id"), row.get("district_id"), row.get("name_uz"), row.get("name_oz"), row.get("name_ru")})
                .toList();
        jdbc.batchUpdate("INSERT INTO villages (id, district_id, name_uz, name_oz, name_ru) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT (id) DO NOTHING", batch);
        log.info("Seeded {} mahallas", batch.size());
    }

    private Integer countOf(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private List<Map<String, Object>> readRows(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, ROWS);
        } catch (Exception e) {
            log.error("Could not read seed data from {}: {}", path, e.getMessage());
            return List.of();
        }
    }
}
