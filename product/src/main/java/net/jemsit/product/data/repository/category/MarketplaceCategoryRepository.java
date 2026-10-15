package net.jemsit.product.data.repository.category;

import net.jemsit.product.data.model.category.MarketplaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceCategoryRepository extends JpaRepository<MarketplaceCategory, Long> {
    List<MarketplaceCategory> findAllByIsDeletedFalseOrderBySortOrderAsc();

    Optional<MarketplaceCategory> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlug(String slug);
}
