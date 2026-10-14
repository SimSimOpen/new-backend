package net.jemsit.common.dto.request.auth;

import net.jemsit.common.annotations.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record RegisterRequestDTO(
        String username,
        @ValidPassword
        String password,
        @Email(message = "Email should be valid")
        String email,
        String agencyName,
        String licenseNumber,
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "Phone number should be valid")
        String phoneNumber
) {
}
