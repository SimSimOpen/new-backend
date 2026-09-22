package net.jemsit.common.dto.request.product.property;

import java.util.List;

public record AddPropertyImagesRequestDTO(Long id, List<String> urls) {
}
