package net.jemsit.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FirebaseLoginRequestDTO(
        @NotBlank(message = "idToken is required")
        String idToken
) {
}
