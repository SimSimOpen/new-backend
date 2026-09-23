package net.jemsit.product.service;

import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.request.product.property.PropertyRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertiesStats;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PropertyService {
    String add(PropertyRequestDTO request);

    PropertyResponseDTO update(Long id, PropertyRequestDTO request);

    Page<PropertyResponseDTO> getAll(Pageable pageable);

    PropertyResponseDTO getById(Long id);

    String deleteById(Long id);

    PropertyResponseDTO addPropertyImage(AddPropertyImagesRequestDTO request, Long userId);

    PropertyResponseDTO createPropertyDraft(Long userId);

    void deletePropertyImage(Long id, Long userId);

    Page<PropertyResponseDTO> getAgentsAllProperties(Pageable pageable);

    Page<PropertyResponseDTO> getAllPublished(Pageable pageable);

    Integer getPropertyMediaCount(Long propertyId);

    PropertiesStats getPropertiesStats();

    long incrementViews(Long id);
}
