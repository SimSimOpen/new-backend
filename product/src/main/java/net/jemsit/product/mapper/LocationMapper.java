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
    RegionsResponseDTO toRegionResponseDTO(Region region);

    DistrictsResponseDTO toDistrictResponseDTO(District district);

    PlacesResponseDTO toPlaceResponseDTO(Place place);
}
