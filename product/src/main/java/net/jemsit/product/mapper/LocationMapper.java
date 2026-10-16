package net.jemsit.product.mapper;

import net.jemsit.product.data.model.location.District;
import net.jemsit.product.data.model.location.Place;
import net.jemsit.product.data.model.location.Region;
import net.jemsit.common.dto.response.product.propeprty.DistrictsResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.PlacesResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.RegionsResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    default RegionsResponseDTO toRegionResponseDTO(Region region) {
        return new RegionsResponseDTO(region.getId(), LocationNames.pick(region.getNameRu(), region.getNameUz()));
    }

    default DistrictsResponseDTO toDistrictResponseDTO(District district) {
        return new DistrictsResponseDTO(district.getId(),
                LocationNames.pick(district.getNameRu(), district.getNameUz()),
                district.getRegionId());
    }

    default PlacesResponseDTO toPlaceResponseDTO(Place place) {
        return new PlacesResponseDTO(place.getId(),
                LocationNames.pick(place.getNameRu(), place.getNameUz()),
                0L,
                place.getDistrictId());
    }
}
