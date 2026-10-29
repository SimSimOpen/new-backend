package net.jemsit.common.dto.response.product.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.service.PriceMode;
import net.jemsit.common.data.enums.service.ServiceCategory;
import net.jemsit.common.data.enums.service.ServiceKind;
import net.jemsit.common.dto.response.product.propeprty.PropertyMedia;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ServiceListingResponseDTO(
        Long id,
        String title,
        String description,
        BigDecimal price,
        PriceMode priceMode,
        @JsonProperty("views") long viewCount,

        ServiceCategory category,
        /** OFFERING or SEEKING; the side of the market the poster published from. */
        ServiceKind kind,
        ListingStatus listingStatus,

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
