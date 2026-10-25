package net.jemsit.common.dto.request.product.secondhand;

import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.OfferType;
import net.jemsit.common.data.enums.secondhand.ItemCondition;
import net.jemsit.common.data.enums.secondhand.SecondHandCategory;

import java.math.BigDecimal;

public record SecondHandRequestDTO(
        String title,
        String description,
        BigDecimal price,

        SecondHandCategory subCategory,
        ItemCondition condition,
        OfferType offerType,
        ListingStatus listingStatus,

        String ownerContact,
        Long regionId,
        Long districtId,
        Long placeId,
        String address,
        Double latitude,
        Double longitude
) {
}
