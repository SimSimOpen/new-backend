package net.jemsit.auth.service.impl;

import net.jemsit.auth.data.model.User;
import net.jemsit.auth.data.repository.UserRepository;
import net.jemsit.auth.dto.AgentContactResponseDTO;
import net.jemsit.auth.dto.AgentResponseDTO;
import net.jemsit.auth.dto.AgentUpdateRequestDTO;
import net.jemsit.auth.dto.UserDetailsRequestDTO;
import net.jemsit.auth.mapper.AuthMapper;
import net.jemsit.auth.service.TelegramUsernameChecker;
import net.jemsit.auth.service.UserService;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.ProfileApi;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.dto.request.auth.ProfileRequestDTO;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.exceptions.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final ProfileApi profileApi;

    private static final Set<String> STAFF =
            Set.of(Roles.ADMIN.name(), Roles.MANAGER.name(), Roles.AGENT.name());

    @Override
    public UserDetailsResponseDTO getUserDetails() {
        return toDetails(currentUser());
    }

    /** Public: a deactivated agent or a client account has no contact to show. */
    @Override
    public AgentContactResponseDTO getAgentContact(Long id) {
        String telegramUsername = userRepository.findById(id)
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .filter(user -> user.getAuthorities().stream().anyMatch(authority -> STAFF.contains(authority.getAuthority())))
                .map(User::getTelegramUsername)
                .orElse(null);
        return new AgentContactResponseDTO(telegramUsername);
    }

    /**
     * The details payload carries the profile as well: it is what an already-authenticated session
     * (for example one handed over from the client app) uses to fill the stored user data, and the
     * profile is where the avatar URL lives.
     */
    private UserDetailsResponseDTO toDetails(User user) {
        return authMapper.toDto(user, findProfile(user.getProfileId()));
    }

    @Override
    @Transactional
    public UserDetailsResponseDTO updateUserDetails(UserDetailsRequestDTO userDetails) {
        var user = currentUser();
        user.setUsername(userDetails.username());
        user.setEmail(userDetails.email());
        if (userDetails.phoneNumber() != null) {
            user.setPhoneNumber(userDetails.phoneNumber().isBlank() ? null : userDetails.phoneNumber().trim());
        }
        // null leaves it as it is; an empty string clears it.
        if (userDetails.telegramUsername() != null) {
            user.setTelegramUsername(TelegramUsernameChecker.normalize(userDetails.telegramUsername()));
        }

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
        return toDetails(userRepository.saveAndFlush(user));
    }

    @Override
    public List<AgentResponseDTO> getAgents() {
        // Soft-deleted agents are included (enabled = false) so they can be reactivated from the list
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(this::isAgent)
                .map(user -> toAgentDto(user, findProfile(user.getProfileId())))
                .toList();
    }

    @Override
    @Transactional
    public AgentResponseDTO updateAgent(Long id, AgentUpdateRequestDTO request) {
        var user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()) && isAgent(u))
                .orElseThrow(() -> new UserException("Agent not found"));

        String username = request.username().trim();
        String email = request.email().trim();
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new UserException("Username is already taken");
        }
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new UserException("Email is already taken");
        }
        String phoneNumber = trimToEmpty(request.phoneNumber());
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber.isEmpty() ? null : phoneNumber);
        if (request.telegramUsername() != null) {
            user.setTelegramUsername(TelegramUsernameChecker.normalize(request.telegramUsername()));
        }

        ProfileRequestDTO profileRequest = new ProfileRequestDTO(
                user.getProfileId(),
                user.getId(),
                null,
                phoneNumber,
                trimToEmpty(request.firstName()),
                trimToEmpty(request.lastName()),
                null,
                null
        );
        ProfileResponseDTO profile;
        if (user.getProfileId() == null) {
            profile = profileApi.createProfile(profileRequest);
            user.setProfileId(profile.id());
        } else {
            profile = profileApi.updateProfile(user.getProfileId(), profileRequest);
        }
        return toAgentDto(userRepository.saveAndFlush(user), profile);
    }

    @Override
    @Transactional
    public AgentResponseDTO deactivateAgent(Long id) {
        var user = findAgent(id);
        if (!Boolean.TRUE.equals(user.getIsDeleted())) {
            user.softDelete();
        }
        return toAgentDto(userRepository.saveAndFlush(user), findProfile(user.getProfileId()));
    }

    @Override
    @Transactional
    public AgentResponseDTO reactivateAgent(Long id) {
        var user = findAgent(id);
        user.setIsDeleted(false);
        user.setDeletedAt(null);
        return toAgentDto(userRepository.saveAndFlush(user), findProfile(user.getProfileId()));
    }

    private User findAgent(Long id) {
        return userRepository.findById(id)
                .filter(this::isAgent)
                .orElseThrow(() -> new UserException("Agent not found"));
    }

    private boolean isAgent(User user) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> Roles.AGENT.name().equals(authority.getAuthority()));
    }

    private AgentResponseDTO toAgentDto(User user, ProfileResponseDTO profile) {
        return new AgentResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber() != null || profile == null ? user.getPhoneNumber() : profile.phoneNumber(),
                user.getTelegramUsername(),
                authMapper.mapAuthoritiesToRoles(user.getAuthorities()),
                !Boolean.TRUE.equals(user.getIsDeleted()),
                user.getCreatedAt(),
                user.getDeletedAt(),
                profile
        );
    }

    // updateProfile skips nulls, so an empty string is what clears a field the admin emptied
    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private ProfileResponseDTO findProfile(Long profileId) {
        if (profileId == null) {
            return null;
        }
        try {
            return profileApi.getProfileById(profileId);
        } catch (RuntimeException e) {
            log.warn("Profile {} could not be loaded: {}", profileId, e.getMessage());
            return null;
        }
    }

    private User currentUser() {
        return userRepository.findById(UserContext.getUserId())
                .orElseThrow(() -> new UserException("User not found"));
    }
}
