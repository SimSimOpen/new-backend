package net.jemsit.product.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.product.data.model.category.MarketplaceCategory;
import net.jemsit.product.data.repository.category.MarketplaceCategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

/**
 * Inserts the marketplace categories that don't exist yet. Existing rows are left alone, so edits
 * made through the admin endpoint survive restarts; adding a category to the JSON adds it on the
 * next boot.
 */
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class MarketplaceCategorySeeder implements ApplicationRunner {

    private static final String SEED_FILE = "data/marketplace-categories.json";

    private final ObjectMapper objectMapper;
    private final MarketplaceCategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<MarketplaceCategory> missing = readSeed().stream()
                .filter(category -> !categoryRepository.existsBySlug(category.getSlug()))
                .toList();
        if (!missing.isEmpty()) {
            categoryRepository.saveAll(missing);
            log.info("Seeded {} marketplace categories", missing.size());
        }
    }

    private List<MarketplaceCategory> readSeed() {
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<MarketplaceCategory>>() {
            });
        } catch (Exception e) {
            log.error("Could not read seed data from {}: {}", SEED_FILE, e.getMessage());
            return List.of();
        }
    }
}
