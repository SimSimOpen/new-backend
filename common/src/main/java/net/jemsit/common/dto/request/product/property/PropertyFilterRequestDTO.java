package net.jemsit.common.dto.request.product.property;

import net.jemsit.common.data.enums.property.*;

import java.math.BigDecimal;
import java.util.List;

public  record PropertyFilterRequestDTO(
        String search,
        PropertyCategory category,
        PropertyType type,
        OfferType offerType,
        ListingStatus listingStatus,
        OccupancyStatus occupancyStatus,
        Long regionId,
        Long districtId,
        Long placeId,
        List<PropertyType> types,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        Integer roomsFrom,
        Integer floorFrom,
        List<String> amenities
) {
}