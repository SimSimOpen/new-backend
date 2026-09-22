package net.jemsit.common.dto.request.product.property;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PropertyLocation (
         String mapLocation,
         String country,
         @JsonProperty("region_id") Long regionID,
         @JsonProperty("district_id") Long districtID,
         @JsonProperty("place_id")  Long placeID,
         String address
) {
}
