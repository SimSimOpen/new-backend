package net.jemsit.product.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Seeds the location reference data shipped in {@code product/src/main/resources/data}.
 *
 * <p>The dataset is versioned. Once the rows on disk are in the database, {@link #DATASET_VERSION} is
 * recorded in {@code seed_meta}; later boots are no-ops. A boot that finds a different version (or
 * none) rebuilds the whole hierarchy in one transaction and drops the region/district/place refs of
 * existing listings first: ids are not stable across datasets, so a surviving ref would silently
 * resolve to an unrelated place. Listings keep every other field and re-acquire a location on the
 * next edit.
 *
 * <p>Equivalent manual SQL, to apply the same change without a redeploy:
 *
 * <pre>{@code
 * UPDATE property_locations SET region_id = NULL, district_id = NULL, place_id = NULL
 *  WHERE region_id IS NOT NULL OR district_id IS NOT NULL OR place_id IS NOT NULL;
 * DELETE FROM villages;
 * DELETE FROM districts;
 * DELETE FROM regions;
 * -- insert the new rows here --
 * SELECT setval(pg_get_serial_sequence('regions', 'id'),   (SELECT COALESCE(MAX(id), 0) + 1 FROM regions), false);
 * SELECT setval(pg_get_serial_sequence('districts', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM districts), false);
 * SELECT setval(pg_get_serial_sequence('villages', 'id'),  (SELECT COALESCE(MAX(id), 0) + 1 FROM villages), false);
 * INSERT INTO seed_meta (key, value) VALUES ('location_dataset', 'tashkent-v1')
 *  ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;
 * }</pre>
 *
 * <p>Rows appended to the JSON without touching existing ids don't need any of that: bump
 * {@link #ADDITIONS_VERSION} instead and the next boot inserts only the missing rows, leaving every
 * listing's location intact.
 */
@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private static final TypeReference<List<Map<String, Object>>> ROWS = new TypeReference<>() {
    };

    // Bump whenever the shipped JSON changes in a way that makes stored ids meaningless.
    private static final String DATASET_VERSION = "tashkent-v1";
    private static final String DATASET_KEY = "location_dataset";
    // Bump whenever rows are appended to the JSON and every existing id keeps its meaning.
    private static final String ADDITIONS_VERSION = "tashkent-v1-yandex-places";
    private static final String ADDITIONS_KEY = "location_additions";

    // Oldest first: villages reference districts, districts reference regions.
    private static final List<String> LOCATION_TABLES = List.of("regions", "districts", "villages");

    private static final String CREATE_SEED_META =
            "CREATE TABLE IF NOT EXISTS seed_meta (key text PRIMARY KEY, value text NOT NULL)";

    private static final String CLEAR_LOCATION_REFS = """
            UPDATE property_locations SET region_id = NULL, district_id = NULL, place_id = NULL
             WHERE region_id IS NOT NULL OR district_id IS NOT NULL OR place_id IS NOT NULL""";

    private static final String MARK_DATASET = """
            INSERT INTO seed_meta (key, value) VALUES (?, ?)
            ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value""";

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute(CREATE_SEED_META);
        if (DATASET_VERSION.equals(datasetVersion())) {
            refillEmptyTables();
            appendNewRows();
            return;
        }
        replaceDataset();
    }

    private void appendNewRows() {
        if (ADDITIONS_VERSION.equals(metaValue(ADDITIONS_KEY))) {
            return;
        }
        tx.executeWithoutResult(status -> {
            int districts = 0;
            for (Map<String, Object> row : readRows("data/districts.json")) {
                districts += jdbc.update("""
                        INSERT INTO districts (id, region_id, name_uz, name_oz, name_ru) VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO NOTHING""",
                        row.get("id"), row.get("region_id"), row.get("name_uz"), row.get("name_oz"), row.get("name_ru"));
            }
            int places = 0;
            for (Map<String, Object> row : readRows("data/villages.json")) {
                places += jdbc.update("""
                        INSERT INTO villages (id, district_id, name_uz, name_oz, name_ru) VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO NOTHING""",
                        row.get("id"), row.get("district_id"), row.get("name_uz"), row.get("name_oz"), row.get("name_ru"));
            }
            alignSequence("districts");
            alignSequence("villages");
            jdbc.update(MARK_DATASET, ADDITIONS_KEY, ADDITIONS_VERSION);
            log.info("Location additions {}: added {} districts, {} places", ADDITIONS_VERSION, districts, places);
        });
    }

    private void replaceDataset() {
        tx.executeWithoutResult(status -> {
            int detached = jdbc.update(CLEAR_LOCATION_REFS);
            for (String table : LOCATION_TABLES) {
                jdbc.execute("DELETE FROM " + table);
            }
            seedRegions();
            seedDistricts();
            seedVillages();
            LOCATION_TABLES.forEach(this::alignSequence);
            jdbc.update(MARK_DATASET, DATASET_KEY, DATASET_VERSION);
            jdbc.update(MARK_DATASET, ADDITIONS_KEY, ADDITIONS_VERSION);
            log.info("Location dataset {}: detached {} listing locations, seeded {} regions, {} districts, {} places",
                    DATASET_VERSION, detached, countOf("regions"), countOf("districts"), countOf("villages"));
        });
    }

    private void refillEmptyTables() {
        if (isEmpty("regions")) {
            seedRegions();
            alignSequence("regions");
            log.info("Seeded {} regions", countOf("regions"));
        }
        if (isEmpty("districts")) {
            seedDistricts();
            alignSequence("districts");
            log.info("Seeded {} districts", countOf("districts"));
        }
        if (isEmpty("villages")) {
            seedVillages();
            alignSequence("villages");
            log.info("Seeded {} villages", countOf("villages"));
        }
    }

    private String datasetVersion() {
        return metaValue(DATASET_KEY);
    }

    private String metaValue(String key) {
        List<String> values = jdbc.queryForList("SELECT value FROM seed_meta WHERE key = ?", String.class, key);
        return values.isEmpty() ? null : values.get(0);
    }

    private boolean isEmpty(String table) {
        return countOf(table) == 0;
    }

    // The seed files carry explicit ids, which leave the identity sequence behind; without this the
    // first runtime insert would collide with a seeded row.
    private void alignSequence(String table) {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), "
                + "(SELECT COALESCE(MAX(id), 0) + 1 FROM " + table + "), false)");
    }

    private void seedRegions() {
        for (Map<String, Object> row : readRows("data/regions.json")) {
            jdbc.update("INSERT INTO regions (id, name_uz, name_oz, name_ru) VALUES (?, ?, ?, ?)",
                    row.get("id"), row.get("name_uz"), row.get("name_oz"), row.get("name_ru"));
        }
    }

    private void seedDistricts() {
        for (Map<String, Object> row : readRows("data/districts.json")) {
            jdbc.update("INSERT INTO districts (id, region_id, name_uz, name_oz, name_ru) VALUES (?, ?, ?, ?, ?)",
                    row.get("id"), row.get("region_id"), row.get("name_uz"), row.get("name_oz"), row.get("name_ru"));
        }
    }

    private void seedVillages() {
        for (Map<String, Object> row : readRows("data/villages.json")) {
            jdbc.update("INSERT INTO villages (id, district_id, name_uz, name_oz, name_ru) VALUES (?, ?, ?, ?, ?)",
                    row.get("id"), row.get("district_id"), row.get("name_uz"), row.get("name_oz"), row.get("name_ru"));
        }
    }

    private Integer countOf(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    // Unreadable seed data must abort the migration: seeding nothing and still stamping the version
    // would leave an empty hierarchy that later boots consider up to date.
    private List<Map<String, Object>> readRows(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, ROWS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read seed data from " + path, e);
        }
    }
}
