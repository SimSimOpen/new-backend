package net.jemsit.product.api;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.api.ProductApi;
import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import net.jemsit.product.service.PropertyService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductApiImpl implements ProductApi {

    private final PropertyService propertyService;

    @Override
    public PropertyResponseDTO createPropertyDraft(Long userId) {
        return propertyService.createPropertyDraft(userId);
    }

    @Override
    public PropertyResponseDTO addPropertyImage(AddPropertyImagesRequestDTO request, Long userId) {
        return propertyService.addPropertyImage(request, userId);
    }
}
