package net.jemsit.common.dto.request.product.services;

import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.service.ServiceCategory;
import net.jemsit.common.data.enums.service.ServiceKind;

import java.math.BigDecimal;
import java.util.List;

public record ServiceListingFilterRequestDTO(
        String search,
        ListingStatus listingStatus,
        /** Which side of the market to list; null means both. */
        ServiceKind kind,
        List<ServiceCategory> categories,
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
