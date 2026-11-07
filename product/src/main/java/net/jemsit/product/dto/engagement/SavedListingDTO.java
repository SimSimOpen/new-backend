package net.jemsit.product.dto.engagement;

import net.jemsit.product.data.model.engagement.ListingKind;

public record SavedListingDTO(ListingKind kind, Long listingId) {
}
