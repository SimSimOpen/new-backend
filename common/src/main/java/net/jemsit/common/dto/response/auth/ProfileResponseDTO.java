package net.jemsit.common.dto.response.auth;

import java.time.LocalDateTime;

public record ProfileResponseDTO(
        Long id,
        Long userId,
        String profileId,
        String phoneNumber,
        String firstName,
        String lastName,
        String profileImageUrl,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
