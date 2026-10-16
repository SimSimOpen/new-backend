package net.jemsit.common.dto.response.dashboard;

import java.util.List;

/** Listing numbers for the admin dashboard, scoped to the caller's own listings unless they are an admin. */
public record ListingDashboardDTO(
        long total,
        long active,
        long draft,
        long archived,
        long available,
        long rented,
        long sold,
        long totalViews,
        List<MonthlyCountDTO> createdPerMonth,
        List<TypeCount> byType,
        List<TopListing> mostViewed,
        List<ActivityItemDTO> activity
) {
    public record TypeCount(String type, long count) {
    }

    public record TopListing(Long id, String title, long views, String listingStatus) {
    }
}
