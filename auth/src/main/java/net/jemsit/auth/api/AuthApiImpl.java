package net.jemsit.auth.api;

import net.jemsit.auth.data.model.User;
import net.jemsit.auth.data.repository.UserRepository;
import net.jemsit.auth.mapper.AuthMapper;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.exceptions.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthApiImpl implements AuthApi {

    private final UserRepository userRepository;
    private final AuthMapper authMapper;

    @Override
    public UserDetailsResponseDTO getById(Long id) {
        return authMapper.toDto(requireUser(id));
    }

    @Override
    public UserDetailsResponseDTO getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException("User not found with username: " + username));
        return authMapper.toDto(user);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found with id: " + id));
    }
}
