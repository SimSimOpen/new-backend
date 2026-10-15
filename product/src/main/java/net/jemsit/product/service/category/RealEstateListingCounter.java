package net.jemsit.product.service.category;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.product.data.repository.property.PropertyRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealEstateListingCounter implements CategoryListingCounter {

    public static final String SLUG = "real-estate";

    private final PropertyRepository propertyRepository;

    @Override
    public String categorySlug() {
        return SLUG;
    }

    @Override
    public long countPublished() {
        return propertyRepository.countByListingStatus(ListingStatus.ACTIVE);
    }
}
