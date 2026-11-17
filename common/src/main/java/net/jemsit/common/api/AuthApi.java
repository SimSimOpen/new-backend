package net.jemsit.common.api;

import net.jemsit.common.dto.response.auth.AuthenticationResponseDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;

import java.util.List;

public interface AuthApi {

    UserDetailsResponseDTO getById(Long id);

    UserDetailsResponseDTO getByUsername(String username);

    AuthenticationResponseDTO authenticateWithOtp(String phoneNumber);

    /** Ids of the active ADMIN accounts: they all receive support chat messages. */
    List<Long> adminIds();
}
