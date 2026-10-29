package net.jemsit.product.service;

import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.request.product.services.ServiceListingFilterRequestDTO;
import net.jemsit.common.dto.request.product.services.ServiceListingRequestDTO;
import net.jemsit.common.dto.response.product.services.ServiceListingResponseDTO;
import net.jemsit.common.dto.response.product.services.ServiceListingStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceListingService {
    ServiceListingResponseDTO createDraft(Long userId);

    ServiceListingResponseDTO update(Long id, ServiceListingRequestDTO request);

    ServiceListingResponseDTO getById(Long id);

    String deleteById(Long id);

    void requireEditable(Long id);

    ServiceListingResponseDTO addImages(AddPropertyImagesRequestDTO request, Long userId);

    void deleteImage(Long id, Long userId);

    Integer getMediaCount(Long listingId);

    Page<ServiceListingResponseDTO> getAgentsAll(Pageable pageable);

    Page<ServiceListingResponseDTO> filter(ServiceListingFilterRequestDTO filter, Pageable pageable);

    ServiceListingStats getStats();

    long incrementViews(Long id);
}
