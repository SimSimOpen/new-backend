package net.jemsit.profile.api;

import net.jemsit.common.api.ProfileApi;
import net.jemsit.common.dto.request.auth.ProfileRequestDTO;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;
import net.jemsit.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileApiImpl implements ProfileApi {

    private final ProfileService profileService;

    @Override
    public ProfileResponseDTO createProfile(ProfileRequestDTO request) {
        return profileService.createProfile(request);
    }

    @Override
    public ProfileResponseDTO getProfileById(Long id) {
        return profileService.getById(id);
    }

    @Override
    public ProfileResponseDTO updateProfile(Long id, ProfileRequestDTO request) {
        return profileService.updateProfile(id, request);
    }
}
