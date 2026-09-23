package net.jemsit.auth.service;

import net.jemsit.auth.dto.UserDetailsRequestDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;

public interface UserService {
    UserDetailsResponseDTO getUserDetails();

    UserDetailsResponseDTO updateUserDetails(UserDetailsRequestDTO userDetails);

}
