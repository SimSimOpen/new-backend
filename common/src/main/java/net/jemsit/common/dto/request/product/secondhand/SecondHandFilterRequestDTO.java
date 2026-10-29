package net.jemsit.common.dto.request.product.secondhand;

import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.OfferType;
import net.jemsit.common.data.enums.secondhand.ItemCondition;
import net.jemsit.common.data.enums.secondhand.SecondHandCategory;

import java.math.BigDecimal;
import java.util.List;

public record SecondHandFilterRequestDTO(
        String search,
        ListingStatus listingStatus,
        OfferType offerType,
        List<SecondHandCategory> subCategories,
        List<ItemCondition> conditions,
        Long regionId,
        Long districtId,
        Long placeId,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        /**
         * Opt-in to "my listings", sent by the admin panel only. Without it the caller's identity is
         * ignored, so an agent browsing the client sees the same public list as any visitor.
         */
        Boolean mine
) {
}
