package net.jemsit.common.dto.request.product.property;

import net.jemsit.common.data.enums.property.*;

import java.math.BigDecimal;

public record PropertyRequestDTO(
        Long id,
        String title,
        String description,
        BigDecimal price,
        Integer numberOfRooms,
        Double area,
        Integer floor,
        Integer totalFloors,
        String publish,

        PropertyCategory category,
        PropertyType type,
        OfferType offerType,
        ListingStatus listingStatus,
        OccupancyStatus occupancyStatus,

        PropertyAmenities amenities,
        String ownerContact,
        String agent,
        Long agentID,
        PropertyLocation location
        ) {
}
