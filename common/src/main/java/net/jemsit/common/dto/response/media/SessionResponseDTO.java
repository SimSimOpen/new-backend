package net.jemsit.common.dto.response.media;

import java.time.Instant;

public record SessionResponseDTO(
        String sessionId,
        Long userId,
        Instant expiresAt
) {
}
