package net.jemsit.common.api;

import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;

public interface AuthApi {

    UserDetailsResponseDTO getById(Long id);

    UserDetailsResponseDTO getByUsername(String username);
}
