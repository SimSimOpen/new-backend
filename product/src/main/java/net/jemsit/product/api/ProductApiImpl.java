package net.jemsit.product.api;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.api.ProductApi;
import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.response.product.car.CarResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import net.jemsit.common.dto.response.product.secondhand.SecondHandResponseDTO;
import net.jemsit.product.service.CarService;
import net.jemsit.product.service.PropertyService;
import net.jemsit.product.service.SecondHandService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductApiImpl implements ProductApi {

    private final PropertyService propertyService;
    private final CarService carService;
    private final SecondHandService secondHandService;

    @Override
    public PropertyResponseDTO createPropertyDraft(Long userId) {
        return propertyService.createPropertyDraft(userId);
    }

    @Override
    public PropertyResponseDTO addPropertyImage(AddPropertyImagesRequestDTO request, Long userId) {
        return propertyService.addPropertyImage(request, userId);
    }

    @Override
    public CarResponseDTO createCarDraft(Long userId) {
        return carService.createDraft(userId);
    }

    @Override
    public void requireCarEditable(Long carId) {
        carService.requireEditable(carId);
    }

    @Override
    public CarResponseDTO addCarImage(AddPropertyImagesRequestDTO request, Long userId) {
        return carService.addImages(request, userId);
    }

    @Override
    public SecondHandResponseDTO createSecondHandDraft(Long userId) {
        return secondHandService.createDraft(userId);
    }

    @Override
    public void requireSecondHandEditable(Long itemId) {
        secondHandService.requireEditable(itemId);
    }

    @Override
    public SecondHandResponseDTO addSecondHandImage(AddPropertyImagesRequestDTO request, Long userId) {
        return secondHandService.addImages(request, userId);
    }
}
