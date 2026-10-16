package net.jemsit.product.controller;

import lombok.RequiredArgsConstructor;
import net.jemsit.product.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("listings")
    public ResponseEntity<?> getListingDashboard() {
        return ResponseEntity.ok(dashboardService.getListingDashboard());
    }
}
