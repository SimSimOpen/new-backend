package net.jemsit.common.dto.response.product.car;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.jemsit.common.data.enums.car.*;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.OccupancyStatus;
import net.jemsit.common.data.enums.property.OfferType;
import net.jemsit.common.dto.response.product.propeprty.PropertyMedia;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CarResponseDTO(
        Long id,
        String title,
        String description,
        BigDecimal price,
        @JsonProperty("views") long viewCount,

        String brand,
        String model,
        Integer year,
        Integer mileage,
        Double engineVolume,
        String color,
        Boolean customsCleared,

        CarBodyType bodyType,
        FuelType fuelType,
        Transmission transmission,
        DriveType driveType,
        CarCondition condition,
        OfferType offerType,
        ListingStatus listingStatus,
        OccupancyStatus occupancyStatus,

        String ownerContact,
        String agent,
        Long agentID,
        Long regionId,
        Long districtId,
        Long placeId,
        List<String> location, //{region-name, district-name, place-name, address} in the request language
        Double latitude,
        Double longitude,
        List<PropertyMedia> medias,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
