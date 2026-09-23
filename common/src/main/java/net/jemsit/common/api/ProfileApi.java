package net.jemsit.common.api;

import net.jemsit.common.dto.request.auth.ProfileRequestDTO;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;

public interface ProfileApi {

    ProfileResponseDTO createProfile(ProfileRequestDTO request);

    ProfileResponseDTO getProfileById(Long id);

    ProfileResponseDTO updateProfile(Long id, ProfileRequestDTO request);
}
