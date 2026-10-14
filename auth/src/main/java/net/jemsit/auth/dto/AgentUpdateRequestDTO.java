package net.jemsit.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AgentUpdateRequestDTO(
        @NotBlank(message = "Username is required")
        @Size(min = 3, message = "Username should have at least 3 characters")
        String username,
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,
        String firstName,
        String lastName,
        @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,20}$", message = "Phone number should be valid")
        String phoneNumber
) {
}
