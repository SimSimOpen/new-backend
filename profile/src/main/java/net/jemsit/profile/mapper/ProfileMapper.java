package net.jemsit.profile.mapper;

import net.jemsit.common.dto.request.auth.ProfileRequestDTO;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;
import net.jemsit.profile.data.model.UserProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    @BeanMapping(unmappedSourcePolicy = ReportingPolicy.IGNORE)
    UserProfile toEntity(ProfileRequestDTO request);

    @BeanMapping(unmappedSourcePolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "profileImageUrl", expression = "java(userProfile.getProfileImageUrl() == null ? null : mediaBaseURL + userProfile.getProfileImageUrl())")
    ProfileResponseDTO toDTO(UserProfile userProfile, String mediaBaseURL);
}
