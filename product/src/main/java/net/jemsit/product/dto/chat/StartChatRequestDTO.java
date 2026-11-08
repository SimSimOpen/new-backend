package net.jemsit.product.dto.chat;

import jakarta.validation.constraints.NotNull;
import net.jemsit.product.data.model.engagement.ListingKind;

public record StartChatRequestDTO(@NotNull ListingKind kind, @NotNull Long listingId) {
}
