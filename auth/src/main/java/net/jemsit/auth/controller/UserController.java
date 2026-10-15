package net.jemsit.auth.controller;

import jakarta.validation.Valid;
import net.jemsit.auth.dto.AgentUpdateRequestDTO;
import net.jemsit.auth.dto.UserDetailsRequestDTO;
import net.jemsit.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("admin/agents")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getAgents() {
        return ResponseEntity.ok(userService.getAgents());
    }

    @GetMapping("admin/clients")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getClients(@RequestParam(defaultValue = "") String search,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.getClients(search, page, size));
    }

    @PutMapping("admin/agents/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateAgent(@PathVariable Long id, @Valid @RequestBody AgentUpdateRequestDTO request) {
        return ResponseEntity.ok(userService.updateAgent(id, request));
    }

    @DeleteMapping("admin/agents/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deactivateAgent(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateAgent(id));
    }

    @PostMapping("admin/agents/{id}/reactivate")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> reactivateAgent(@PathVariable Long id) {
        return ResponseEntity.ok(userService.reactivateAgent(id));
    }
}
