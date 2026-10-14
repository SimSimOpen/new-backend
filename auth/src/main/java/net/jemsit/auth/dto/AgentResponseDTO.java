package net.jemsit.auth.dto;

import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public record AgentResponseDTO(
        Long id,
        String username,
        String email,
        String phoneNumber,
        List<Roles> authorities,
        Boolean enabled,
        LocalDateTime createdAt,
        ProfileResponseDTO profile
) {
}
