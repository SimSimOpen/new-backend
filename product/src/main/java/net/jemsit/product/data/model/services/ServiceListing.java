package net.jemsit.product.data.model.services;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.service.PriceMode;
import net.jemsit.common.data.enums.service.ServiceCategory;
import net.jemsit.common.data.enums.service.ServiceKind;
import net.jemsit.product.data.model.BaseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A services listing: one row carries the general details, the services taxonomy, which side of the
 * market the poster is on, the owner/agent and a single location, with images in a child table.
 * Mirrors {@code SecondHandItem}'s shape, minus the sold/available concept - a service is not stock,
 * so there is nothing to mark as gone.
 */
@Entity
@Table(name = "service_listings")
@Getter
@Setter
public class ServiceListing extends BaseEntity {
    // general details
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    /** The quote for an OFFERING, the budget for a SEEKING; null together with NEGOTIABLE means "make me an offer". */
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    @Column(name = "price_mode")
    private PriceMode priceMode;
    @Column(name = "view_count", nullable = false, columnDefinition = "bigint default 0")
    private long viewCount;

    // listing details
    @Enumerated(EnumType.STRING)
    private ServiceCategory category;
    /** Which side of the market this row was posted from; see {@link ServiceKind}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_kind", nullable = false)
    private ServiceKind kind = ServiceKind.OFFERING;
    @Enumerated(EnumType.STRING)
    private ListingStatus listingStatus;

    // owner or agent details
    private String agent;
    private Long agentID;
    private String ownerOrAgentContact;

    // location details: a listing has a single service area, so the ids live on the row
    @Column(name = "region_id")
    private Long regionId;
    @Column(name = "district_id")
    private Long districtId;
    @Column(name = "place_id")
    private Long placeId;
    private String address;
    private Double latitude;
    private Double longitude;

    // images
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ServiceListingMedia> medias = new ArrayList<>();

    public void addMedia(ServiceListingMedia media) {
        this.medias.add(media);
        media.setListing(this);
    }
}
