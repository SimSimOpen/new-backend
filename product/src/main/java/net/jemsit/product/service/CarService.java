package net.jemsit.product.service;

import net.jemsit.common.dto.request.product.car.CarFilterRequestDTO;
import net.jemsit.common.dto.request.product.car.CarRequestDTO;
import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.response.product.car.CarBrandDto;
import net.jemsit.common.dto.response.product.car.CarResponseDTO;
import net.jemsit.common.dto.response.product.car.CarsStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CarService {
    CarResponseDTO createDraft(Long userId);

    CarResponseDTO update(Long id, CarRequestDTO request);

    CarResponseDTO getById(Long id);

    String deleteById(Long id);

    void requireEditable(Long id);

    CarResponseDTO addImages(AddPropertyImagesRequestDTO request, Long userId);

    void deleteImage(Long id, Long userId);

    Integer getMediaCount(Long carId);

    Page<CarResponseDTO> getAgentsAll(Pageable pageable);

    Page<CarResponseDTO> filter(CarFilterRequestDTO filter, Pageable pageable);

    CarsStats getStats();

    long incrementViews(Long id);

    List<CarBrandDto> getBrands();
}
