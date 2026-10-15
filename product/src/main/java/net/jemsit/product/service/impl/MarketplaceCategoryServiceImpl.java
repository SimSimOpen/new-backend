package net.jemsit.product.service.impl;

import net.jemsit.common.dto.request.product.category.MarketplaceCategoryUpdateRequestDTO;
import net.jemsit.common.dto.response.product.category.MarketplaceCategoryResponseDTO;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.product.data.model.category.MarketplaceCategory;
import net.jemsit.product.data.repository.category.MarketplaceCategoryRepository;
import net.jemsit.product.service.MarketplaceCategoryService;
import net.jemsit.product.service.category.CategoryListingCounter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MarketplaceCategoryServiceImpl implements MarketplaceCategoryService {

    private final MarketplaceCategoryRepository categoryRepository;
    private final Map<String, CategoryListingCounter> countersBySlug;

    public MarketplaceCategoryServiceImpl(MarketplaceCategoryRepository categoryRepository,
                                          List<CategoryListingCounter> counters) {
        this.categoryRepository = categoryRepository;
        this.countersBySlug = counters.stream()
                .collect(Collectors.toMap(CategoryListingCounter::categorySlug, Function.identity()));
    }

    @Override
    public List<MarketplaceCategoryResponseDTO> getAll() {
        return categoryRepository.findAllByIsDeletedFalseOrderBySortOrderAsc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public MarketplaceCategoryResponseDTO getBySlug(String slug) {
        return toResponseDTO(findBySlug(slug));
    }

    @Override
    @Transactional
    public MarketplaceCategoryResponseDTO update(String slug, MarketplaceCategoryUpdateRequestDTO request) {
        MarketplaceCategory category = findBySlug(slug);
        if (request.title() != null) category.setTitle(request.title());
        if (request.tagline() != null) category.setTagline(request.tagline());
        if (request.description() != null) category.setDescription(request.description());
        if (request.imageUrl() != null) category.setImageUrl(request.imageUrl());
        if (request.sortOrder() != null) category.setSortOrder(request.sortOrder());
        if (request.available() != null) {
            // A category can only go live once something can actually be listed in it.
            if (request.available() && !countersBySlug.containsKey(slug)) {
                throw new UserException("Category " + slug + " has no listings support yet and cannot be made available");
            }
            category.setAvailable(request.available());
        }
        return toResponseDTO(categoryRepository.save(category));
    }

    private MarketplaceCategory findBySlug(String slug) {
        return categoryRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new UserException("Category not found: " + slug));
    }

    private MarketplaceCategoryResponseDTO toResponseDTO(MarketplaceCategory category) {
        CategoryListingCounter counter = countersBySlug.get(category.getSlug());
        long listingCount = Boolean.TRUE.equals(category.getAvailable()) && counter != null
                ? counter.countPublished()
                : 0;
        return new MarketplaceCategoryResponseDTO(
                category.getSlug(),
                category.getTitle(),
                category.getTagline(),
                category.getDescription(),
                category.getImageUrl(),
                Boolean.TRUE.equals(category.getAvailable()),
                category.getSortOrder(),
                listingCount
        );
    }
}
