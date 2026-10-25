package net.jemsit.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.dto.request.product.secondhand.SecondHandFilterRequestDTO;
import net.jemsit.common.dto.request.product.secondhand.SecondHandRequestDTO;
import net.jemsit.product.service.SecondHandService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/second-hand")
public class SecondHandController {

    private final SecondHandService secondHandService;

    @PostMapping("create-draft")
    public ResponseEntity<?> createDraft() {
        return ResponseEntity.ok(secondHandService.createDraft(UserContext.getUserId()));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody SecondHandRequestDTO request) {
        return ResponseEntity.ok(secondHandService.update(id, request));
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(secondHandService.getById(id));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        return ResponseEntity.ok(secondHandService.deleteById(id));
    }

    @DeleteMapping("delete/image/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id) {
        secondHandService.deleteImage(id, UserContext.getUserId());
        return ResponseEntity.ok("Image deleted successfully");
    }

    @GetMapping("media-count/{item-id}")
    public Integer getMediaCount(@PathVariable("item-id") Long itemId) {
        return secondHandService.getMediaCount(itemId);
    }

    @GetMapping("agents-all")
    public ResponseEntity<?> getAgentsAll(Pageable pageable) {
        return ResponseEntity.ok(secondHandService.getAgentsAll(pageable));
    }

    @GetMapping("filter")
    public ResponseEntity<?> filter(SecondHandFilterRequestDTO filterRequest, Pageable pageable) {
        return ResponseEntity.ok(secondHandService.filter(filterRequest, pageable));
    }

    @GetMapping("stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(secondHandService.getStats());
    }

    @PostMapping("{id}/views")
    public ResponseEntity<?> incrementViews(@PathVariable Long id) {
        return ResponseEntity.ok(secondHandService.incrementViews(id));
    }
}
