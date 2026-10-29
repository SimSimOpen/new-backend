package net.jemsit.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.dto.request.product.services.ServiceListingFilterRequestDTO;
import net.jemsit.common.dto.request.product.services.ServiceListingRequestDTO;
import net.jemsit.product.service.ServiceListingService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/services")
public class ServiceListingController {

    private final ServiceListingService serviceListingService;

    @PostMapping("create-draft")
    public ResponseEntity<?> createDraft() {
        return ResponseEntity.ok(serviceListingService.createDraft(UserContext.getUserId()));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ServiceListingRequestDTO request) {
        return ResponseEntity.ok(serviceListingService.update(id, request));
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceListingService.getById(id));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceListingService.deleteById(id));
    }

    @DeleteMapping("delete/image/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id) {
        serviceListingService.deleteImage(id, UserContext.getUserId());
        return ResponseEntity.ok("Image deleted successfully");
    }

    @GetMapping("media-count/{listing-id}")
    public Integer getMediaCount(@PathVariable("listing-id") Long listingId) {
        return serviceListingService.getMediaCount(listingId);
    }

    @GetMapping("agents-all")
    public ResponseEntity<?> getAgentsAll(Pageable pageable) {
        return ResponseEntity.ok(serviceListingService.getAgentsAll(pageable));
    }

    @GetMapping("filter")
    public ResponseEntity<?> filter(ServiceListingFilterRequestDTO filterRequest, Pageable pageable) {
        return ResponseEntity.ok(serviceListingService.filter(filterRequest, pageable));
    }

    @GetMapping("stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(serviceListingService.getStats());
    }

    @PostMapping("{id}/views")
    public ResponseEntity<?> incrementViews(@PathVariable Long id) {
        return ResponseEntity.ok(serviceListingService.incrementViews(id));
    }
}
