package net.jemsit.product.service.category;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.product.data.repository.services.ServiceListingRepository;
import org.springframework.stereotype.Component;

/**
 * Publishes the services live-listing count to the category cards. Its presence is also what lets
 * {@code MarketplaceCategoryServiceImpl} flip the category {@code available} - without a counter for
 * the slug, going live is refused. Both sides of the market count together: a visitor picking a side
 * narrows the list afterwards.
 */
@Component
@RequiredArgsConstructor
public class ServiceListingCounter implements CategoryListingCounter {

    public static final String SLUG = "services";

    private final ServiceListingRepository listingRepository;

    @Override
    public String categorySlug() {
        return SLUG;
    }

    @Override
    public long countPublished() {
        return listingRepository.countByListingStatus(ListingStatus.ACTIVE);
    }
}
