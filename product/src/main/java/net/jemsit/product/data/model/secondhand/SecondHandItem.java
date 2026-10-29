package net.jemsit.product.data.model.secondhand;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.OccupancyStatus;
import net.jemsit.common.data.enums.property.OfferType;
import net.jemsit.common.data.enums.secondhand.ItemCondition;
import net.jemsit.common.data.enums.secondhand.SecondHandCategory;
import net.jemsit.product.data.model.BaseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A pre-loved item for sale. Mirrors {@code Car}'s shape: one row carries the general details, the
 * item-specific taxonomy, the owner/agent and a single location, with images in a child table.
 */
@Entity
@Table(name = "second_hand_items")
@Getter
@Setter
public class SecondHandItem extends BaseEntity {
    // general details
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    private BigDecimal price;
    @Column(name = "view_count", nullable = false, columnDefinition = "bigint default 0")
    private long viewCount;

    // item details
    @Enumerated(EnumType.STRING)
    @Column(name = "sub_category")
    private SecondHandCategory subCategory;
    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition")
    private ItemCondition condition;
    @Enumerated(EnumType.STRING)
    private OfferType offerType;
    @Enumerated(EnumType.STRING)
    private ListingStatus listingStatus;
    /**
     * Whether the item is still on the market. Only AVAILABLE and SOLD are used here (RENTED is a
     * car/property concept); rows written before this field existed are null and count as available.
     */
    @Enumerated(EnumType.STRING)
    private OccupancyStatus occupancyStatus;

    // owner or agent details
    private String agent;
    private Long agentID;
    private String ownerOrAgentContact;

    // location details: an item has a single spot, so the ids live on the row instead of a joined table
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
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<SecondHandMediaData> medias = new ArrayList<>();

    public void addMedia(SecondHandMediaData media) {
        this.medias.add(media);
        media.setItem(this);
    }
}
