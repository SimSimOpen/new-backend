package net.jemsit.common.dto.response.product.propeprty;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PropertiesStats(@JsonProperty("total_count") Long totalCount,
                              @JsonProperty("active_count") Long activeCount,
                              @JsonProperty("rented_count") Long rentedCount,
                              @JsonProperty("draft_count") Long draftCount) {
}
