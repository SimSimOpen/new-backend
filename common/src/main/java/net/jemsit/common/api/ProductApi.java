package net.jemsit.common.api;

import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;

public interface ProductApi {

    PropertyResponseDTO createPropertyDraft(Long userId);

    PropertyResponseDTO addPropertyImage(AddPropertyImagesRequestDTO request, Long userId);
}
