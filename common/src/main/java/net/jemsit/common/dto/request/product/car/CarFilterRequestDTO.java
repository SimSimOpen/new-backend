package net.jemsit.common.dto.request.product.car;

import net.jemsit.common.data.enums.car.*;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.OccupancyStatus;
import net.jemsit.common.data.enums.property.OfferType;

import java.math.BigDecimal;
import java.util.List;

public record CarFilterRequestDTO(
        String search,
        String brand,
        String model,
        ListingStatus listingStatus,
        OfferType offerType,
        OccupancyStatus occupancyStatus,
        CarCondition condition,
        List<CarBodyType> bodyTypes,
        List<FuelType> fuelTypes,
        List<Transmission> transmissions,
        List<DriveType> driveTypes,
        Long regionId,
        Long districtId,
        Long placeId,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        Integer yearFrom,
        Integer yearTo,
        Integer mileageTo
) {
}
