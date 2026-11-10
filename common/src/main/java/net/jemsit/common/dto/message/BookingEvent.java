package net.jemsit.common.dto.message;

import java.util.List;

/**
 * A booking was requested, answered or cancelled. Published by the product module, pushed by the
 * notification module over /api/event/stream as an SSE event named {@code booking}.
 */
public record BookingEvent(List<Long> recipientIds, Object payload) {
}
