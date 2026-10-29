package net.jemsit.common.dto.response.product.secondhand;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.OccupancyStatus;
import net.jemsit.common.data.enums.property.OfferType;
import net.jemsit.common.data.enums.secondhand.ItemCondition;
import net.jemsit.common.data.enums.secondhand.SecondHandCategory;
import net.jemsit.common.dto.response.product.propeprty.PropertyMedia;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SecondHandResponseDTO(
        Long id,
        String title,
        String description,
        BigDecimal price,
        @JsonProperty("views") long viewCount,

        SecondHandCategory subCategory,
        ItemCondition condition,
        OfferType offerType,
        ListingStatus listingStatus,
        /** AVAILABLE or SOLD; null on rows created before the field existed, meaning available. */
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
