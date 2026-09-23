package net.jemsit.auth.dto;

public record UserDetailsRequestDTO(
        String username,
        String email,
        String fullName,
        String phoneNumber,
        String description
) {
}
