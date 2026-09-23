package net.jemsit.profile.service.impl;

import net.jemsit.common.dto.message.UserAvatarUpdated;
import net.jemsit.common.dto.request.auth.ProfileRequestDTO;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.profile.data.model.UserProfile;
import net.jemsit.profile.data.repository.UserProfileRepository;
import net.jemsit.profile.mapper.ProfileMapper;
import net.jemsit.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ProfileMapper profileMapper;
    @Value("${app.media.base-url}")
    private String mediaBaseURL;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public ProfileResponseDTO createProfile(ProfileRequestDTO request) {
        var profileEntity = profileMapper.toEntity(request);
        profileEntity.setProfileId(generateUniqueProfileCode());
        return profileMapper.toDTO(userProfileRepository.save(profileEntity), mediaBaseURL);
    }

    @Override
    public ProfileResponseDTO getById(Long id) {
        return profileMapper.toDTO(requireProfile(id), mediaBaseURL);
    }

    @Override
    public ProfileResponseDTO updateProfile(Long id, ProfileRequestDTO request) {
        var profile = requireProfile(id);
        if (request.phoneNumber() != null) {
            profile.setPhoneNumber(request.phoneNumber());
        }
        if (request.firstName() != null) {
            profile.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            profile.setLastName(request.lastName());
        }
        if (request.description() != null) {
            profile.setDescription(request.description());
        }
        return profileMapper.toDTO(userProfileRepository.save(profile), mediaBaseURL);
    }

    private String generateUniqueProfileCode() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            String code = String.valueOf(100_000 + RANDOM.nextInt(900_000));
            if (!userProfileRepository.existsByProfileId(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate unique profile code, consider expanding to 7 digits");
    }

    @Transactional
    @EventListener
    public void onUserAvatarUpdated(UserAvatarUpdated event) {
        updateProfileAvatar(Long.valueOf(event.getUserId()), event.getUserAvatarUrl());
    }

    private void updateProfileAvatar(Long userId, String avatarUrl) {
        var userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException("User not found with id: " + userId));
        userProfile.setProfileImageUrl(avatarUrl);
        userProfileRepository.save(userProfile);
        log.info("Updated avatar for userProfile {}: {}", userId, avatarUrl);
    }

    private UserProfile requireProfile(Long id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new UserException("Profile not found with id: " + id));
    }
}
