package net.jemsit.product.data.model.category;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.jemsit.product.data.model.BaseEntity;

/**
 * A top-level section of the marketplace (real estate, cars, services, ...), shown as a card on the
 * client's home page. Not to be confused with {@code PropertyCategory}, which classifies a property
 * inside the real-estate section.
 */
@Entity
@Table(name = "marketplace_categories")
@Getter
@Setter
public class MarketplaceCategory extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String slug;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(length = 150)
    private String tagline;
    @Column(length = 500)
    private String description;
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    @Column(nullable = false)
    private Boolean available = false;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
