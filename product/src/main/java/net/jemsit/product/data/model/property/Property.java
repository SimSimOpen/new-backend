package net.jemsit.product.data.model.property;

import net.jemsit.common.data.enums.property.*;
import net.jemsit.product.data.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "properties")
@Getter
@Setter
public class Property extends BaseEntity {
    //general details
    private String title;
    private String description;
    private BigDecimal price;
    private Integer numberOfRooms;
    private Integer floor;
    private Integer totalFloors;
    private Double area;
    private String publish;
    @Column(name = "view_count", nullable = false, columnDefinition = "bigint default 0")
    private long viewCount;

    @Enumerated(EnumType.STRING)
    private PropertyCategory category;
    @Enumerated(EnumType.STRING)
    private PropertyType type;
    @Enumerated(EnumType.STRING)
    private OfferType offerType;
    @Enumerated(EnumType.STRING)
    private ListingStatus listingStatus;
    @Enumerated(EnumType.STRING)
    private OccupancyStatus occupancyStatus;

    // owner or agent details
    private String agent;
    private Long agentID;
    private String ownerOrAgentContact;


    //location details
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private PropertyLocation location;

    //amenities
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private PropertyAmenities amenities;

    //images
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyMediaData> medias = new ArrayList<>();

    public void addMedia(PropertyMediaData media) {
        this.medias.add(media);
        media.setProperty(this);
    }

    @Override
    public String toString() {
        return "Property{" +
                "title='" + title + '\'' +
                ", description=" + description +
                ", price=" + price +
                ", numberOfRooms=" + numberOfRooms +
                ", area=" + area +
                ", location=" + location +
                ", medias=" + medias.stream().map(m -> m.getId() + ":" + m.getMediaURL()).toList() +
                '}';
    }
}
