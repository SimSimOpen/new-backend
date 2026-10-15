package net.jemsit.auth.service.impl;

import net.jemsit.auth.data.model.Token;
import net.jemsit.auth.data.model.User;
import net.jemsit.auth.data.repository.UserRepository;
import net.jemsit.auth.mapper.AuthMapper;
import net.jemsit.auth.service.AuthService;
import net.jemsit.auth.service.JwtService;
import net.jemsit.common.api.OperatorFeedApi;
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
    // Interface from :common, implementation in the root shell - this module never sees :notify.
    private final OperatorFeedApi operatorFeedApi;

    @Transactional
    @Override
    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO requestDTO) {
        var user = userRepository.findByUsername(requestDTO.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + requestDTO.username()));
        if (!passwordEncoder.matches(requestDTO.password(), user.getPassword())) {
            throw new UserException("Password is incorrect");
        }
        requireActive(user);
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
        operatorFeedApi.accountRegistered(Roles.CLIENT, savedUser.getUsername());
    }

    @Override
    public void registerAgent(RegisterRequestDTO request) {
        var userExists = userRepository.findByUsernameOrEmail(request.username(), request.email());
        if (userExists.isPresent()) {
            throw new UserException("User with given username or email already exists");
        }
        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setEmail(request.email());
        newUser.setAuthorities(List.of(Roles.AGENT));
        newUser.setPhoneNumber(normalizePhone(request.phoneNumber()));
        var savedUser = userRepository.save(newUser);
        var resp = createProfileForUser(savedUser.getId(), savedUser.getPhoneNumber());
        savedUser.setProfileId(resp.id());
        userRepository.save(savedUser);
        operatorFeedApi.accountRegistered(Roles.AGENT, savedUser.getUsername());
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
            // OTP accounts are keyed by the phone number the code was sent to
            newUser.setPhoneNumber(normalizePhone(requestDTO.username()));
            newUser.setAuthorities(List.of(Roles.CLIENT));
            var savedUser = userRepository.save(newUser);
            var tokenModel = createTokenModel(savedUser);
            savedUser.addToken(tokenModel);
            savedUser.setUpdatedAt(LocalDateTime.now());
            var resp = createProfileForUser(savedUser.getId(), savedUser.getPhoneNumber());
            savedUser.setProfileId(resp.id());
            // This branch provisions a brand-new account rather than authenticating an existing one,
            // and it is reached from GET /api/event/verify-otp through AuthApiImpl - so in practice this
            // is the signup route that actually gets used. Only the creation branch announces anything.
            // Note the username here is the phone number the OTP was sent to, so that is what the
            // operator's card shows.
            operatorFeedApi.accountRegistered(Roles.CLIENT, savedUser.getUsername());
            return authMapper.toDTO(userRepository.saveAndFlush(savedUser), tokenModel);
        } else {
            var existing = user.get();
            requireActive(existing);
            var tokenModel = createTokenModel(existing);
            existing.addToken(tokenModel);
            existing.setUpdatedAt(LocalDateTime.now());
            return authMapper.toDTO(userRepository.saveAndFlush(existing), tokenModel);
        }
    }

    private static void requireActive(User user) {
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new UserException("This account has been deactivated");
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
        return createProfileForUser(userId, null);
    }

    private ProfileResponseDTO createProfileForUser(Long userId, String phoneNumber) {
        ProfileRequestDTO request = new ProfileRequestDTO(
                null,
                userId,
                null,
                phoneNumber,
                null,
                null,
                null,
                null
        );
        return profileApi.createProfile(request);
    }

    private static String normalizePhone(String phoneNumber) {
        return phoneNumber == null || phoneNumber.isBlank() ? null : phoneNumber.trim();
    }

}
