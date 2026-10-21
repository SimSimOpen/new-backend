package net.jemsit.product.service.category;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.product.data.repository.car.CarRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarsListingCounter implements CategoryListingCounter {

    public static final String SLUG = "cars";

    private final CarRepository carRepository;

    @Override
    public String categorySlug() {
        return SLUG;
    }

    @Override
    public long countPublished() {
        return carRepository.countByListingStatus(ListingStatus.ACTIVE);
    }
}
