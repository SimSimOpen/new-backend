package net.jemsit.common.dto.message;

import java.util.List;

/**
 * Something happened in a listing chat that its participants' open browsers should see at once.
 * Published by the product module, pushed by the notification module over /api/event/stream as an
 * SSE event named {@code chat}; the payload is serialized to JSON as it is.
 */
public record ChatEvent(List<Long> recipientIds, Object payload) {
}
