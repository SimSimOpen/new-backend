package net.jemsit.common.dto.response.product.car;

import java.util.List;

/**
 * One catalog brand with its model names, e.g. {@code {"brand": "Chevrolet", "models": ["Cobalt", ...]}}.
 * Feeds the add-car form's brand/model dropdowns; ids stay hidden because listings store
 * brand/model as plain strings and never reference the catalog by key.
 */
public record CarBrandDto(String brand, List<String> models) {
}
