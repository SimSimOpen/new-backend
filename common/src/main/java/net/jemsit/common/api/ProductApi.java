package net.jemsit.common.api;

import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.response.product.car.CarResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;

public interface ProductApi {

    PropertyResponseDTO createPropertyDraft(Long userId);

    PropertyResponseDTO addPropertyImage(AddPropertyImagesRequestDTO request, Long userId);

    CarResponseDTO createCarDraft(Long userId);

    /** Throws unless the current user owns the car or is an admin. */
    void requireCarEditable(Long carId);

    CarResponseDTO addCarImage(AddPropertyImagesRequestDTO request, Long userId);
}
