package net.jemsit.auth.service;

import net.jemsit.common.dto.request.auth.AuthenticationRequestDTO;
import net.jemsit.common.dto.request.auth.RegisterRequestDTO;
import net.jemsit.common.dto.response.auth.AuthenticationResponseDTO;

public interface AuthService {
    AuthenticationResponseDTO authenticate(AuthenticationRequestDTO requestDTO);

    void registerClient(RegisterRequestDTO request);

    void registerAgent(RegisterRequestDTO request);

    void registerAdmin(RegisterRequestDTO request);

    AuthenticationResponseDTO authenticateWithOtp(AuthenticationRequestDTO request);
}
