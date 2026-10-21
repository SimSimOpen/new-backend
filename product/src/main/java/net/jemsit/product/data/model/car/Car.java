package net.jemsit.product.data.model.car;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.jemsit.common.data.enums.car.*;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.OccupancyStatus;
import net.jemsit.common.data.enums.property.OfferType;
import net.jemsit.product.data.model.BaseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cars")
@Getter
@Setter
public class Car extends BaseEntity {
    //general details
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    private BigDecimal price;
    @Column(name = "view_count", nullable = false, columnDefinition = "bigint default 0")
    private long viewCount;

    //vehicle details
    private String brand;
    private String model;
    @Column(name = "production_year")
    private Integer year;
    private Integer mileage;
    private Double engineVolume;
    private String color;
    private Boolean customsCleared;

    @Enumerated(EnumType.STRING)
    private CarBodyType bodyType;
    @Enumerated(EnumType.STRING)
    private FuelType fuelType;
    @Enumerated(EnumType.STRING)
    private Transmission transmission;
    @Enumerated(EnumType.STRING)
    private DriveType driveType;
    @Enumerated(EnumType.STRING)
    @Column(name = "car_condition")
    private CarCondition condition;
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

    // location details: a car has a single spot, so the ids live on the row instead of a joined table
    @Column(name = "region_id")
    private Long regionId;
    @Column(name = "district_id")
    private Long districtId;
    @Column(name = "place_id")
    private Long placeId;
    private String address;
    private Double latitude;
    private Double longitude;

    //images
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CarMediaData> medias = new ArrayList<>();

    public void addMedia(CarMediaData media) {
        this.medias.add(media);
        media.setCar(this);
    }
}
