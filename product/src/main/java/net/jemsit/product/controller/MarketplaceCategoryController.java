package net.jemsit.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jemsit.common.dto.request.product.category.MarketplaceCategoryUpdateRequestDTO;
import net.jemsit.product.service.MarketplaceCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/category")
public class MarketplaceCategoryController {

    private final MarketplaceCategoryService categoryService;

    @GetMapping("all")
    public ResponseEntity<?> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("{slug}")
    public ResponseEntity<?> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getBySlug(slug));
    }

    @PutMapping("{slug}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateCategory(@PathVariable String slug,
                                            @Valid @RequestBody MarketplaceCategoryUpdateRequestDTO request) {
        return ResponseEntity.ok(categoryService.update(slug, request));
    }
}
