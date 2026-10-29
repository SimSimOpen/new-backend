package net.jemsit.common.dto.request.product.services;

import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.service.PriceMode;
import net.jemsit.common.data.enums.service.ServiceCategory;
import net.jemsit.common.data.enums.service.ServiceKind;

import java.math.BigDecimal;

/**
 * Saves a services listing field by field: a null means "leave what is stored alone", which is what
 * lets the admin form publish a draft that only has photos so far.
 */
public record ServiceListingRequestDTO(
        String title,
        String description,
        BigDecimal price,
        PriceMode priceMode,
        ServiceCategory category,
        /** OFFERING (I provide this) or SEEKING (I need this). */
        ServiceKind kind,
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
