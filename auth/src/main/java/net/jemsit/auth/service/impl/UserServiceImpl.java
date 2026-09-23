package net.jemsit.auth.service.impl;

import net.jemsit.auth.data.model.User;
import net.jemsit.auth.data.repository.UserRepository;
import net.jemsit.auth.dto.UserDetailsRequestDTO;
import net.jemsit.auth.mapper.AuthMapper;
import net.jemsit.auth.service.UserService;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.ProfileApi;
import net.jemsit.common.dto.request.auth.ProfileRequestDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.exceptions.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final ProfileApi profileApi;

    @Override
    public UserDetailsResponseDTO getUserDetails() {
        return authMapper.toDto(currentUser());
    }

    @Override
    @Transactional
    public UserDetailsResponseDTO updateUserDetails(UserDetailsRequestDTO userDetails) {
        var user = currentUser();
        user.setUsername(userDetails.username());
        user.setEmail(userDetails.email());

        Pair<String, String> fullName = userDetails.fullName().split(" ").length > 1 ?
                Pair.of(userDetails.fullName().split(" ")[0], userDetails.fullName().split(" ")[1]) :
                Pair.of(userDetails.fullName(), "");

        ProfileRequestDTO profileRequest = new ProfileRequestDTO(
                user.getProfileId(),
                user.getId(),
                null,
                userDetails.phoneNumber(),
                fullName.getFirst(),
                fullName.getSecond(),
                null,
                userDetails.description()
        );
        profileApi.updateProfile(user.getProfileId(), profileRequest);
        return authMapper.toDto(userRepository.saveAndFlush(user));
    }

    private User currentUser() {
        return userRepository.findById(UserContext.getUserId())
                .orElseThrow(() -> new UserException("User not found"));
    }
}
