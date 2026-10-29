package net.jemsit.common.dto.response.product.services;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Counters for the services section header. The offering/seeking split is the one that matters here:
 * it is how an agent sees whether their side of the market is the busy one.
 */
public record ServiceListingStats(@JsonProperty("total_count") Long totalCount,
                                  @JsonProperty("active_count") Long activeCount,
                                  @JsonProperty("draft_count") Long draftCount,
                                  @JsonProperty("offering_count") Long offeringCount,
                                  @JsonProperty("seeking_count") Long seekingCount) {
}
