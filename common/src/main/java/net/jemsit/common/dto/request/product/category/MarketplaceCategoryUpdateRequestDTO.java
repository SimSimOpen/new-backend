package net.jemsit.common.dto.request.product.category;

import jakarta.validation.constraints.Size;

/**
 * Partial update of a marketplace category. Null fields are left unchanged.
 */
public record MarketplaceCategoryUpdateRequestDTO(
        @Size(max = 100) String title,
        @Size(max = 150) String tagline,
        @Size(max = 500) String description,
        @Size(max = 1000) String imageUrl,
        Boolean available,
        Integer sortOrder
) {
}
