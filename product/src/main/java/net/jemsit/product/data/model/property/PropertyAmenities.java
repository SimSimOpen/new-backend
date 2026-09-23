package net.jemsit.product.data.model.property;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "property_amenities")
@Getter
@Setter
public class PropertyAmenities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Boolean hasParking;
    private Boolean hasGarden;
    private Boolean hasSwimmingPool;
    private Boolean hasGym;
    private Boolean hasSecurity;
    private Boolean hasElevator;
    private Boolean hasWashingMachine;
    private Boolean hasAirConditioning;
    private Boolean hasInternet;
    private Boolean hasRefrigerator;
    private Boolean hasDishwasher;
    private Boolean hasMicrowave;
    private Boolean hasParkingSpace;
    private Boolean hasTV;
    private Boolean hasSatellite;
    private Boolean hasFurniture;

}
