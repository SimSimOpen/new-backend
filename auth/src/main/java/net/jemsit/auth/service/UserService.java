package net.jemsit.auth.service;

import net.jemsit.auth.dto.AgentResponseDTO;
import net.jemsit.auth.dto.AgentUpdateRequestDTO;
import net.jemsit.auth.dto.UserDetailsRequestDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;

import java.util.List;

public interface UserService {
    UserDetailsResponseDTO getUserDetails();

    UserDetailsResponseDTO updateUserDetails(UserDetailsRequestDTO userDetails);

    List<AgentResponseDTO> getAgents();

    AgentResponseDTO updateAgent(Long id, AgentUpdateRequestDTO request);

}
