package net.jemsit.common.dto.request.product.property;

import net.jemsit.common.data.enums.property.*;

public  record PropertyFilterRequestDTO(
        String search,
        PropertyCategory category,
        PropertyType type,
        OfferType offerType,
        ListingStatus listingStatus,
        OccupancyStatus occupancyStatus
) {
}