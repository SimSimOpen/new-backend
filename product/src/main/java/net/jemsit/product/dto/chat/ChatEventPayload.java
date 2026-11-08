package net.jemsit.product.dto.chat;

/** Body of the {@code chat} SSE event: a new message in one conversation. */
public record ChatEventPayload(String type, Long conversationId, ChatMessageDTO message) {
    public static ChatEventPayload message(ChatMessageDTO message) {
        return new ChatEventPayload("message", message.conversationId(), message);
    }
}
