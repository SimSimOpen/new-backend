package net.jemsit.product.controller;

import lombok.RequiredArgsConstructor;
import net.jemsit.product.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/location")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/regions")
    public ResponseEntity<?> getRegions() {
        return ResponseEntity.ok(locationService.getRegions());
    }

    @GetMapping("/districts/{regionId}")
    public ResponseEntity<?> getDistrictsByRegionId(@PathVariable Long regionId) {
        return ResponseEntity.ok(locationService.getDistrictsByRegionId(regionId));
    }

    @GetMapping("/places/{districtId}")
    public ResponseEntity<?> getPlacesByDistrictId(@PathVariable Long districtId) {
        return ResponseEntity.ok(locationService.getPlacesByDistrictId(districtId));
    }
}
