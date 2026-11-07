package net.jemsit.product.dto.engagement;

import net.jemsit.common.dto.response.dashboard.MonthlyCountDTO;

import java.util.List;

public record EngagementStatsDTO(
        long listings,
        long views,
        long saves,
        long shares,
        List<MonthlyCountDTO> savesPerMonth,
        List<MonthlyCountDTO> sharesPerMonth,
        List<ListingRow> topListings
) {
    public record ListingRow(
            String kind,
            long id,
            String title,
            String agent,
            String listingStatus,
            long views,
            long saves,
            long shares
    ) {
    }
}
