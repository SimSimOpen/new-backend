package net.jemsit.auth.dto;

import net.jemsit.common.dto.response.auth.ProfileResponseDTO;

import java.time.LocalDateTime;

public record ClientResponseDTO(
        Long id,
        String username,
        String email,
        String phoneNumber,
        /** PHONE for accounts created by OTP sign-in (no password), PASSWORD otherwise. */
        String signInMethod,
        Boolean enabled,
        LocalDateTime createdAt,
        /** Every sign-in saves the user, so this is effectively the last login. */
        LocalDateTime lastActiveAt,
        ProfileResponseDTO profile
) {
}
