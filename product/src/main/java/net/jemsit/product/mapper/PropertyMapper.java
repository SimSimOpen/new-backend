package net.jemsit.product.mapper;

import net.jemsit.common.dto.request.product.property.PropertyRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyMedia;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import net.jemsit.product.data.model.property.Property;
import net.jemsit.product.data.model.property.PropertyMediaData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class PropertyMapper {


    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "agentID", ignore = true)
    @Mapping(target = "agent", ignore = true)
    public abstract Property toEntity(PropertyRequestDTO resident);

    @Mapping(target = "medias", expression = "java(mapMedia(property.getMedias()))")
    @Mapping(target = "location", ignore = true)
    public abstract PropertyResponseDTO toDto(Property property);

    @Mapping(target = "medias", expression = "java(mapMedia(property.getMedias()))")
    @Mapping(target = "location", source = "shortAddress")
    @Mapping(target = "amenities", source = "property.amenities")
    public abstract PropertyResponseDTO toDtoWithShortAddress(Property property, List<String> shortAddress);

    public List<PropertyMedia> mapMedia(List<PropertyMediaData> media) {
        if (media == null) {
            return null;
        }
        return media.stream()
                .map(p -> new PropertyMedia(p.getId(), minioEndpoint + p.getMediaURL(), p.getIsCoverImage()))
                .collect(Collectors.toList());
    }
}
