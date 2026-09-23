package net.jemsit.auth.controller;

import net.jemsit.auth.service.AuthService;
import net.jemsit.common.dto.request.auth.AuthenticationRequestDTO;
import net.jemsit.common.dto.request.auth.RegisterRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequestDTO request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("authenticate/with-otp")
    public ResponseEntity<?> authenticateClientWithOTP(@RequestBody AuthenticationRequestDTO request) {
        return ResponseEntity.ok(authService.authenticateWithOtp(request));
    }

    @PostMapping("register/client")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        authService.registerClient(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("register/agent")
    public ResponseEntity<?> registerAgent(@Valid @RequestBody RegisterRequestDTO request) {
        authService.registerAgent(request);
        return ResponseEntity.ok().build();
    }
}
