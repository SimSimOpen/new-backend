package net.jemsit.common.dto.request.auth;

public record ProfileRequestDTO(
        Long id,
        Long userId,
        String profileId,
        String phoneNumber,
        String firstName,
        String lastName,
        String profileImageUrl,
        String description
) {
}
