package net.jemsit.auth.controller;

import net.jemsit.auth.dto.UserDetailsRequestDTO;
import net.jemsit.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("details")
    public ResponseEntity<?> getUserDetails() {
        return ResponseEntity.ok(userService.getUserDetails());
    }

    @PutMapping("update-user")
    public ResponseEntity<?> updateUser(@RequestBody UserDetailsRequestDTO request) {
        return ResponseEntity.ok(userService.updateUserDetails(request));
    }
}
