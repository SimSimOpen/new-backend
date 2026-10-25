package net.jemsit.product.service.category;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.product.data.repository.secondhand.SecondHandItemRepository;
import org.springframework.stereotype.Component;

/**
 * Publishes the second-hand live-listing count to the category cards. Its presence is also what
 * lets {@code MarketplaceCategoryServiceImpl} flip the category {@code available} - without a
 * counter for the slug, going live is refused.
 */
@Component
@RequiredArgsConstructor
public class SecondHandListingCounter implements CategoryListingCounter {

    public static final String SLUG = "second-hand";

    private final SecondHandItemRepository itemRepository;

    @Override
    public String categorySlug() {
        return SLUG;
    }

    @Override
    public long countPublished() {
        return itemRepository.countByListingStatus(ListingStatus.ACTIVE);
    }
}
