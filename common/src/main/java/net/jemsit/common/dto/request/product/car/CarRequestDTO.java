package net.jemsit.common.dto.request.product.car;

import net.jemsit.common.data.enums.car.*;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.OccupancyStatus;
import net.jemsit.common.data.enums.property.OfferType;

import java.math.BigDecimal;

public record CarRequestDTO(
        String title,
        String description,
        BigDecimal price,

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
        Long regionId,
        Long districtId,
        Long placeId,
        String address,
        Double latitude,
        Double longitude
) {
}
