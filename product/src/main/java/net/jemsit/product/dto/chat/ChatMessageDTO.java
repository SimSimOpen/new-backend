package net.jemsit.product.dto.chat;

import java.time.Instant;

/** {@code senderRole} lets each side tell its own bubbles apart without knowing user ids. */
public record ChatMessageDTO(Long id, Long conversationId, ChatRole senderRole, String body, Instant createdAt) {
}
