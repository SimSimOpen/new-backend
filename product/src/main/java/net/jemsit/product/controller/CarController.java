package net.jemsit.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.dto.request.product.car.CarFilterRequestDTO;
import net.jemsit.common.dto.request.product.car.CarRequestDTO;
import net.jemsit.product.service.CarService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/car")
public class CarController {

    private final CarService carService;

    @PostMapping("create-draft")
    public ResponseEntity<?> createDraft() {
        return ResponseEntity.ok(carService.createDraft(UserContext.getUserId()));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CarRequestDTO request) {
        return ResponseEntity.ok(carService.update(id, request));
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(carService.getById(id));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        return ResponseEntity.ok(carService.deleteById(id));
    }

    @DeleteMapping("delete/image/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id) {
        carService.deleteImage(id, UserContext.getUserId());
        return ResponseEntity.ok("Image deleted successfully");
    }

    @GetMapping("car-media-count/{car-id}")
    public Integer getMediaCount(@PathVariable("car-id") Long carId) {
        return carService.getMediaCount(carId);
    }

    @GetMapping("agents-all")
    public ResponseEntity<?> getAgentsAll(Pageable pageable) {
        return ResponseEntity.ok(carService.getAgentsAll(pageable));
    }

    @GetMapping("filter")
    public ResponseEntity<?> filter(CarFilterRequestDTO filterRequest, Pageable pageable) {
        return ResponseEntity.ok(carService.filter(filterRequest, pageable));
    }

    @GetMapping("stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(carService.getStats());
    }

    @PostMapping("{id}/views")
    public ResponseEntity<?> incrementViews(@PathVariable Long id) {
        return ResponseEntity.ok(carService.incrementViews(id));
    }
}
