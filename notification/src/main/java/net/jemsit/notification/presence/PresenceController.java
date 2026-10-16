package net.jemsit.notification.presence;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import org.springframework.http.ResponseEntity;
import net.jemsit.common.data.enums.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/event/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public record HeartbeatRequest(String app, String visitorId) {
    }

    /** Public: anonymous website visitors count too. A valid token adds the signed-in account. */
    @PostMapping("heartbeat")
    public ResponseEntity<?> heartbeat(@RequestBody HeartbeatRequest request) {
        try {
            presenceService.heartbeat(request.app(), request.visitorId(), UserContext.getUserId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.noContent().build();
    }

    /** Admins and managers only (this module has no Spring Security, so the role is checked here). */
    @GetMapping("stats")
    public ResponseEntity<PresenceStatsDTO> stats() {
        var roles = UserContext.getRoles();
        if (roles == null || !(roles.contains(Roles.ADMIN) || roles.contains(Roles.MANAGER))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(presenceService.stats());
    }
}
