package net.jemsit.product.data.model.services;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "service_listing_images")
@Getter
@Setter
public class ServiceListingMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "media_url", nullable = false)
    private String mediaURL;
    @Column(name = "is_cover_image", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isCoverImage = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private ServiceListing listing;
}
