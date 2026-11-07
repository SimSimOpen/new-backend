package net.jemsit.product.controller;

import lombok.RequiredArgsConstructor;
import net.jemsit.product.data.model.engagement.ListingKind;
import net.jemsit.product.service.EngagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/engagement")
public class EngagementController {

    private final EngagementService engagementService;

    @GetMapping("saved")
    public ResponseEntity<?> getSaved(@RequestParam String visitorId) {
        return ResponseEntity.ok(engagementService.getSaved(visitorId));
    }

    @PutMapping("saved/{kind}/{id}")
    public ResponseEntity<?> save(@PathVariable ListingKind kind, @PathVariable Long id, @RequestParam String visitorId) {
        engagementService.save(kind, id, visitorId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("saved/{kind}/{id}")
    public ResponseEntity<?> unsave(@PathVariable ListingKind kind, @PathVariable Long id, @RequestParam String visitorId) {
        engagementService.unsave(kind, id, visitorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("shares/{kind}/{id}")
    public ResponseEntity<?> recordShare(@PathVariable ListingKind kind, @PathVariable Long id, @RequestParam String visitorId) {
        engagementService.recordShare(kind, id, visitorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("stats")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<?> getStats(@RequestParam(required = false) ListingKind kind,
                                      @RequestParam(defaultValue = "views") String sort) {
        return ResponseEntity.ok(engagementService.getStats(kind, sort));
    }
}
