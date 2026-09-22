package net.jemsit.common.clients.property;

import net.jemsit.common.clients.FeignAuthInterceptor;
import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service", path = "/api/product", configuration = FeignAuthInterceptor.class)
public interface ProductServiceClient {

    @PostMapping("/v1/property/create-draft")
    PropertyResponseDTO createDraftProperty();

    @PostMapping(value = "/v1/property/add/images")
    PropertyResponseDTO addPropertyImage(@RequestBody AddPropertyImagesRequestDTO request);
}
