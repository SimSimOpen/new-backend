package net.jemsit.profile.service;

import net.jemsit.common.dto.request.auth.ProfileRequestDTO;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;

public interface ProfileService {
    ProfileResponseDTO createProfile(ProfileRequestDTO request);

    ProfileResponseDTO getById(Long id);

    ProfileResponseDTO updateProfile(Long id, ProfileRequestDTO request);
}
