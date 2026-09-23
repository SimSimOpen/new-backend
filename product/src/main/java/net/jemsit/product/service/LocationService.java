package net.jemsit.product.service;

import net.jemsit.common.dto.response.product.propeprty.DistrictsResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.PlacesResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.RegionsResponseDTO;

import java.util.List;

public interface LocationService {
    List<RegionsResponseDTO> getRegions();

    List<DistrictsResponseDTO> getDistrictsByRegionId(Long regionId);

    List<PlacesResponseDTO> getPlacesByDistrictId(Long districtId);
}
