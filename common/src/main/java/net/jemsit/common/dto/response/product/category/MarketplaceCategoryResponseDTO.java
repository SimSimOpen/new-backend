package net.jemsit.common.dto.response.product.category;

public record MarketplaceCategoryResponseDTO(
        String slug,
        String title,
        String tagline,
        String description,
        String imageUrl,
        boolean available,
        int sortOrder,
        long listingCount
) {
}
