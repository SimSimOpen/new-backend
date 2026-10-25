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
import java.util.Set;

/**
 * Inserts the marketplace categories that don't exist yet, and force-enables the ones that have
 * launched. Existing rows are otherwise left alone, so copy/ordering edits made through the admin
 * endpoint survive restarts; adding a category to the JSON adds it on the next boot.
 *
 * <p>The insert-only rule has one gap: a category whose row was seeded <em>before</em> it launched
 * stays {@code available=false} forever, because the JSON value is never re-applied to an existing
 * row. Cars hit this on prod - its row predates the launch, so editing the seed to {@code true}
 * changed nothing and the admin kept showing "Not active yet". {@link #LAUNCHED} closes the gap by
 * re-applying {@code available=true} for those slugs on boot, so a deploy (not a manual UPDATE) is
 * what flips prod. Trade-off: an admin toggling a launched category off is reverted on the next
 * restart, which is the intended behaviour for sections meant to stay live.
 */
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class MarketplaceCategorySeeder implements ApplicationRunner {

    private static final String SEED_FILE = "data/marketplace-categories.json";

    /** Slugs that are live and must stay live even if their row was seeded as unavailable. */
    private static final Set<String> LAUNCHED = Set.of("cars", "second-hand");

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
        forceLaunchedCategoriesLive();
    }

    /** See {@link #LAUNCHED}: re-apply available=true for launched slugs whose row predates launch. */
    private void forceLaunchedCategoriesLive() {
        for (String slug : LAUNCHED) {
            categoryRepository.findBySlugAndIsDeletedFalse(slug).ifPresent(category -> {
                if (!Boolean.TRUE.equals(category.getAvailable())) {
                    category.setAvailable(true);
                    categoryRepository.save(category);
                    log.info("Forced launched category '{}' available=true", slug);
                }
            });
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
