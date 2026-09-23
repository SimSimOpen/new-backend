package net.jemsit.product.service;

import net.jemsit.common.dto.request.product.property.PropertyFilterRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PropertyFilterService {
    Page<PropertyResponseDTO> filterProperties(PropertyFilterRequestDTO filterRequest, Pageable pageable);
}
