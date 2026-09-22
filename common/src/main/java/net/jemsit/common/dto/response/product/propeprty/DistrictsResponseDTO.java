package net.jemsit.common.dto.response.product.propeprty;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DistrictsResponseDTO(long id, @JsonProperty("name_en") String nameRu, @JsonProperty("region_id") long regionId) {
}
