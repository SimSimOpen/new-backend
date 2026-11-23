package net.jemsit.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jemsit.product.data.model.engagement.ListingKind;
import net.jemsit.product.data.model.report.ReportStatus;
import net.jemsit.product.dto.report.CreateReportRequestDTO;
import net.jemsit.product.dto.report.ReportStatusRequestDTO;
import net.jemsit.product.service.ReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Complaints about listings. Filing one needs a signed-in user (SecurityConfig leaves this path
 * authenticated); reading and closing them is for admins.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateReportRequestDTO request) {
        return ResponseEntity.ok(reportService.create(request.kind(), request.listingId(), request.reason(),
                request.details()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> list(@RequestParam(required = false) ReportStatus status,
                                  @RequestParam(required = false) ListingKind kind,
                                  @RequestParam(required = false) Long listingId,
                                  Pageable pageable) {
        return ResponseEntity.ok(reportService.list(status, kind, listingId, pageable));
    }

    @GetMapping("open-count")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> openCount() {
        return ResponseEntity.ok(Map.of("count", reportService.openCount()));
    }

    @PostMapping("{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> setStatus(@PathVariable Long id, @Valid @RequestBody ReportStatusRequestDTO request) {
        return ResponseEntity.ok(reportService.setStatus(id, request.status()));
    }
}
