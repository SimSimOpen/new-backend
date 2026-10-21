package net.jemsit.common.dto.response.product.car;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CarsStats(@JsonProperty("total_count") Long totalCount,
                        @JsonProperty("active_count") Long activeCount,
                        @JsonProperty("sold_count") Long soldCount,
                        @JsonProperty("draft_count") Long draftCount) {
}
