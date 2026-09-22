package net.jemsit.common.dto.response.product.propeprty;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.jemsit.common.data.enums.property.*;
import net.jemsit.common.dto.request.product.property.PropertyAmenities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PropertyResponseDTO(
        Long id,
        String title,
        String description,
        BigDecimal price,
        Integer numberOfRooms,
        Double area,
        Integer floor,
        Integer totalFloors,
        String publish,
        @JsonProperty("views") long viewCount,

        PropertyCategory category,
        PropertyType type,
        OfferType offerType,
        ListingStatus listingStatus,
        OccupancyStatus occupancyStatus,

        String ownerContact,
        String agent,
        Long agentID,
        List<String> location, //{place-name, district-name, region-name, address} only in Uzbekistan
        List<PropertyMedia> medias,
        PropertyAmenities amenities,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
