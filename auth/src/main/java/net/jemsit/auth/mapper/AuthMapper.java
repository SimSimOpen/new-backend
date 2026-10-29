package net.jemsit.auth.mapper;

import net.jemsit.auth.data.model.Token;
import net.jemsit.auth.data.model.User;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.dto.response.auth.AuthenticationResponseDTO;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "token", source = "token.token")
    @Mapping(target = "refreshToken", source = "token.refreshToken")
    @Mapping(target = "authorities", source = "user.authorities", qualifiedByName = "mapAuthoritiesToRoles")
    AuthenticationResponseDTO toDTO(User user, Token token);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "token", source = "token.token")
    @Mapping(target = "refreshToken", source = "token.refreshToken")
    @Mapping(target = "authorities", source = "user.authorities", qualifiedByName = "mapAuthoritiesToRoles")
    @Mapping(target = "profile", source = "profile")
    AuthenticationResponseDTO toDTOWithProfile(User user, Token token, ProfileResponseDTO profile);

    @Mapping(target = "roles", source = "user.authorities", qualifiedByName = "mapAuthoritiesToRoles")
    UserDetailsResponseDTO toDto(User user);

    /** Same payload with the profile attached; both sources carry an id, so it is pinned to the user. */
    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "roles", source = "user.authorities", qualifiedByName = "mapAuthoritiesToRoles")
    @Mapping(target = "profile", source = "profile")
    UserDetailsResponseDTO toDto(User user, ProfileResponseDTO profile);


    @Named("mapAuthoritiesToRoles")
    default List<Roles> mapAuthoritiesToRoles(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return null;
        }
        return authorities.stream()
                .map(authority -> Roles.valueOf(authority.getAuthority()))
                .collect(Collectors.toList());
    }
}
