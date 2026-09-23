package net.jemsit.auth.service.impl;

import net.jemsit.auth.data.model.Token;
import net.jemsit.auth.data.model.User;
import net.jemsit.auth.data.repository.UserRepository;
import net.jemsit.auth.mapper.AuthMapper;
import net.jemsit.auth.service.AuthService;
import net.jemsit.auth.service.JwtService;
import net.jemsit.common.api.ProfileApi;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.dto.request.auth.AuthenticationRequestDTO;
import net.jemsit.common.dto.request.auth.ProfileRequestDTO;
import net.jemsit.common.dto.request.auth.RegisterRequestDTO;
import net.jemsit.common.dto.response.auth.AuthenticationResponseDTO;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;
import net.jemsit.common.exceptions.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final ProfileApi profileApi;

    @Transactional
    @Override
    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO requestDTO) {
        var user = userRepository.findByUsername(requestDTO.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + requestDTO.username()));
        if (!passwordEncoder.matches(requestDTO.password(), user.getPassword())) {
            throw new UserException("Password is incorrect");
        }
        var tokenModel = createTokenModel(user);
        user.addToken(tokenModel);
        user.setUpdatedAt(LocalDateTime.now());
        var userProfile = profileApi.getProfileById(user.getProfileId());
        return authMapper.toDTOWithProfile(userRepository.saveAndFlush(user), tokenModel, userProfile);
    }

    @Override
    public void registerClient(RegisterRequestDTO request) {
        var userExists = userRepository.findByUsernameOrEmail(request.username(), request.email());
        if (userExists.isPresent()) {
            throw new UserException("User with given username or email already exists");
        }
        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setEmail(request.email());
        newUser.setAuthorities(List.of(Roles.CLIENT));
        var savedUser = userRepository.save(newUser);
        var resp = createProfileForUser(savedUser.getId());
        savedUser.setProfileId(resp.id());
        userRepository.save(savedUser);
    }

    @Override
    public void registerAgent(RegisterRequestDTO request) {
        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setEmail(request.email());
        newUser.setAuthorities(List.of(Roles.AGENT));
        var savedUser = userRepository.save(newUser);
        var resp = createProfileForUser(savedUser.getId());
        savedUser.setProfileId(resp.id());
        userRepository.save(savedUser);
    }

    @Override
    public void registerAdmin(RegisterRequestDTO request) {
        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setEmail(request.email());
        newUser.setAuthorities(List.of(Roles.ADMIN));
        var savedUser = userRepository.save(newUser);
        var resp = createProfileForUser(savedUser.getId());
        savedUser.setProfileId(resp.id());
        userRepository.save(savedUser);
    }


    @Override
    public AuthenticationResponseDTO authenticateWithOtp(AuthenticationRequestDTO requestDTO) {
        var user = userRepository.findByUsername(requestDTO.username());
        if (user.isEmpty()) {
            User newUser = new User();
            newUser.setUsername(requestDTO.username());
            newUser.setAuthorities(List.of(Roles.CLIENT));
            var savedUser = userRepository.save(newUser);
            var tokenModel = createTokenModel(savedUser);
            savedUser.addToken(tokenModel);
            savedUser.setUpdatedAt(LocalDateTime.now());
            var resp = createProfileForUser(savedUser.getId());
            savedUser.setProfileId(resp.id());
            return authMapper.toDTO(userRepository.saveAndFlush(savedUser), tokenModel);
        } else {
            var existing = user.get();
            var tokenModel = createTokenModel(existing);
            existing.addToken(tokenModel);
            existing.setUpdatedAt(LocalDateTime.now());
            return authMapper.toDTO(userRepository.saveAndFlush(existing), tokenModel);
        }
    }

    private Token createTokenModel(User user) {
        var token = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        Token tokenModel = new Token();
        tokenModel.setToken(token);
        tokenModel.setRefreshToken(refreshToken);
        tokenModel.setUser(user);
        return tokenModel;
    }

    private ProfileResponseDTO createProfileForUser(Long userId) {
        ProfileRequestDTO request = new ProfileRequestDTO(
                null,
                userId,
                null,
                null,
                null,
                null,
                null,
                null
        );
        return profileApi.createProfile(request);
    }

}
