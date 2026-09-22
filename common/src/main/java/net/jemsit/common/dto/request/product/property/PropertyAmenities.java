package net.jemsit.common.dto.request.product.property;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PropertyAmenities(
        @JsonProperty("parking")
        Boolean hasParking,
        @JsonProperty("garden")
        Boolean hasGarden,
        @JsonProperty("swimmingPool")
        Boolean hasSwimmingPool,
        @JsonProperty("gym")
        Boolean hasGym,
        @JsonProperty("security")
        Boolean hasSecurity,
        @JsonProperty("elevator")
        Boolean hasElevator,
        @JsonProperty("washingMachine")
        Boolean hasWashingMachine,
        @JsonProperty("airConditioning")
        Boolean hasAirConditioning,
        @JsonProperty("internet")
        Boolean hasInternet,
        @JsonProperty("refrigerator")
        Boolean hasRefrigerator,
        @JsonProperty("dishwasher")
        Boolean hasDishwasher,
        @JsonProperty("microwave")
        Boolean hasMicrowave,
        @JsonProperty("parkingSpace")
        Boolean hasParkingSpace,
        @JsonProperty("tv")
        Boolean hasTV,
        @JsonProperty("satellite")
        Boolean hasSatellite,
        @JsonProperty("furniture")
        Boolean hasFurniture
) {
}
