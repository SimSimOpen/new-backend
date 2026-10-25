package net.jemsit.product.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.product.data.model.car.CarBrand;
import net.jemsit.product.data.model.car.CarModel;
import net.jemsit.product.data.repository.car.CarBrandRepository;
import net.jemsit.product.data.repository.car.CarModelRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Inserts the car brand/model catalog entries that don't exist yet, mirroring
 * {@link MarketplaceCategorySeeder}. Existing brands are skipped entirely - including their
 * model lists - so manual edits survive restarts; a brand added to the JSON appears on the next
 * boot. Seed data: data/car-brands.json.
 */
@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class CarCatalogSeeder implements ApplicationRunner {

    private static final String SEED_FILE = "data/car-brands.json";

    private final ObjectMapper objectMapper;
    private final CarBrandRepository brandRepository;
    private final CarModelRepository modelRepository;

    /** One {@code {"brand": ..., "models": [...]}} entry of the seed file. */
    record SeedBrand(String brand, List<String> models) {
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SeedBrand> missing = readSeed().stream()
                .filter(entry -> entry.brand() != null && !entry.brand().isBlank())
                .filter(entry -> !brandRepository.existsByNameIgnoreCase(entry.brand().trim()))
                .toList();
        if (missing.isEmpty()) {
            return;
        }
        List<CarModel> models = new ArrayList<>();
        for (SeedBrand entry : missing) {
            CarBrand brand = new CarBrand();
            brand.setName(entry.brand().trim());
            brand = brandRepository.save(brand);
            if (entry.models() == null) {
                continue;
            }
            for (String name : entry.models()) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                CarModel model = new CarModel();
                model.setName(name.trim());
                model.setBrand(brand);
                models.add(model);
            }
        }
        modelRepository.saveAll(models);
        log.info("Seeded {} car brands with {} models", missing.size(), models.size());
    }

    private List<SeedBrand> readSeed() {
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<SeedBrand>>() {
            });
        } catch (Exception e) {
            log.error("Could not read seed data from {}: {}", SEED_FILE, e.getMessage());
            return List.of();
        }
    }
}
