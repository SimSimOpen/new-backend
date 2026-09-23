package net.jemsit.product.data.model.property;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "property_locations")
@Getter
@Setter
public class PropertyLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mapLocation;
    private String country;
    @Column(name = "region_id")
    private Long regionID;
    @Column(name = "district_id")
    private Long districtID;
    @Column(name = "place_id")
    private Long placeID;
    private String address;
}
