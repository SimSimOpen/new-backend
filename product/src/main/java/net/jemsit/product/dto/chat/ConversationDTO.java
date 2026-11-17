package net.jemsit.product.dto.chat;

import net.jemsit.product.data.model.engagement.ListingKind;

import java.time.Instant;

/**
 * A conversation as the caller sees it: {@code role} is the caller's side, {@code counterpartName}
 * the other side (the agent for a client, the client for an agent). A {@code support} conversation
 * is with the SimSim admins: it has no kind, listing or title, and the client sees no counterpart name.
 */
public record ConversationDTO(
        Long id,
        boolean support,
        ListingKind kind,
        Long listingId,
        String listingTitle,
        ChatRole role,
        String counterpartName,
        String lastMessage,
        ChatRole lastSenderRole,
        Instant lastMessageAt,
        long unread
) {
}
