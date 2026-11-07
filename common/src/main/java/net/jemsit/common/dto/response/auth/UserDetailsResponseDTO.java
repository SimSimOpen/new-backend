package net.jemsit.common.dto.response.auth;

import net.jemsit.common.data.enums.Roles;

import java.util.List;

public record UserDetailsResponseDTO(
        Long id,
        String username,
        String email,
        String telegramUsername,
        List<Roles> roles,
        ProfileResponseDTO profile
) {
}
