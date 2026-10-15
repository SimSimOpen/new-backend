package net.jemsit.product.service;

import net.jemsit.common.dto.request.product.category.MarketplaceCategoryUpdateRequestDTO;
import net.jemsit.common.dto.response.product.category.MarketplaceCategoryResponseDTO;

import java.util.List;

public interface MarketplaceCategoryService {
    List<MarketplaceCategoryResponseDTO> getAll();

    MarketplaceCategoryResponseDTO getBySlug(String slug);

    MarketplaceCategoryResponseDTO update(String slug, MarketplaceCategoryUpdateRequestDTO request);
}
