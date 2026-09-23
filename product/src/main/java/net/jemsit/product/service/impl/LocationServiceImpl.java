package net.jemsit.product.service.impl;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.dto.response.product.propeprty.DistrictsResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.PlacesResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.RegionsResponseDTO;
import net.jemsit.product.data.repository.location.DistrictRepository;
import net.jemsit.product.data.repository.location.PlaceRepository;
import net.jemsit.product.data.repository.location.RegionRepository;
import net.jemsit.product.mapper.LocationMapper;
import net.jemsit.product.service.LocationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final PlaceRepository placeRepository;
    private final LocationMapper locationMapper;

    @Override
    public List<RegionsResponseDTO> getRegions() {
        return regionRepository.findAll().stream().map(locationMapper::toRegionResponseDTO).toList();
    }

    @Override
    public List<DistrictsResponseDTO> getDistrictsByRegionId(Long regionId) {
        return districtRepository.findByRegionId(regionId).stream().map(locationMapper::toDistrictResponseDTO).toList();
    }

    @Override
    public List<PlacesResponseDTO> getPlacesByDistrictId(Long districtId) {
        return placeRepository.findByDistrictId(districtId).stream().map(locationMapper::toPlaceResponseDTO).toList();
    }
}
