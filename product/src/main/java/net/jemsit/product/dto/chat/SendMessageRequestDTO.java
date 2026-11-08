package net.jemsit.product.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.jemsit.product.data.model.chat.ChatMessage;

public record SendMessageRequestDTO(@NotBlank @Size(max = ChatMessage.MAX_LENGTH) String body) {
}
