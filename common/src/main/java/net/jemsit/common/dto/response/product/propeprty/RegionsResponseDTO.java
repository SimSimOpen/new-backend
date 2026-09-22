package net.jemsit.common.dto.response.product.propeprty;


import com.fasterxml.jackson.annotation.JsonProperty;

public record RegionsResponseDTO(long id, @JsonProperty("name_en") String nameRu) {
}
