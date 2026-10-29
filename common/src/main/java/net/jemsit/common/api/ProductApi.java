package net.jemsit.common.api;

import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.response.product.car.CarResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import net.jemsit.common.dto.response.product.secondhand.SecondHandResponseDTO;
import net.jemsit.common.dto.response.product.services.ServiceListingResponseDTO;

public interface ProductApi {

    PropertyResponseDTO createPropertyDraft(Long userId);

    PropertyResponseDTO addPropertyImage(AddPropertyImagesRequestDTO request, Long userId);

    CarResponseDTO createCarDraft(Long userId);

    /** Throws unless the current user owns the car or is an admin. */
    void requireCarEditable(Long carId);

    CarResponseDTO addCarImage(AddPropertyImagesRequestDTO request, Long userId);

    SecondHandResponseDTO createSecondHandDraft(Long userId);

    /** Throws unless the current user owns the second-hand item or is an admin. */
    void requireSecondHandEditable(Long itemId);

    SecondHandResponseDTO addSecondHandImage(AddPropertyImagesRequestDTO request, Long userId);

    ServiceListingResponseDTO createServiceDraft(Long userId);

    /** Throws unless the current user owns the services listing or is an admin. */
    void requireServiceEditable(Long listingId);

    ServiceListingResponseDTO addServiceImage(AddPropertyImagesRequestDTO request, Long userId);
}
