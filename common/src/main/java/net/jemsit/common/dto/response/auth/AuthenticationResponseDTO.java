package net.jemsit.common.dto.response.auth;

import net.jemsit.common.data.enums.Roles;

import java.util.List;

public record  AuthenticationResponseDTO (
        Long id,
        String username,
        String email,
        List<Roles> authorities,
        String token,
        String refreshToken,
        ProfileResponseDTO profile
) {
}
