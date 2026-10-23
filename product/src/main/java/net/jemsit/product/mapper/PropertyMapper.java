package net.jemsit.product.mapper;

import net.jemsit.common.dto.request.product.property.PropertyRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyMedia;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import net.jemsit.product.data.model.property.Property;
import net.jemsit.product.data.model.property.PropertyLocation;
import net.jemsit.product.data.model.property.PropertyMediaData;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
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
    @Mapping(target = "ownerOrAgentContact", source = "ownerContact")
    public abstract Property toEntity(PropertyRequestDTO resident);

    // The response DTO exposes coordinates flat, and clients echo that shape back, so the generated
    // nested mapping of location.latitude/longitude alone would silently persist NULL.
    @AfterMapping
    protected void applyCoordinates(PropertyRequestDTO resident, @MappingTarget Property property) {
        if (resident.latitude() == null && resident.longitude() == null) {
            return;
        }
        PropertyLocation location = property.getLocation();
        if (location == null) {
            location = new PropertyLocation();
            property.setLocation(location);
        }
        if (resident.latitude() != null) {
            location.setLatitude(resident.latitude());
        }
        if (resident.longitude() != null) {
            location.setLongitude(resident.longitude());
        }
    }

    @Mapping(target = "medias", expression = "java(mapMedia(property.getMedias()))")
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "latitude", source = "location.latitude")
    @Mapping(target = "longitude", source = "location.longitude")
    @Mapping(target = "ownerContact", source = "ownerOrAgentContact")
    public abstract PropertyResponseDTO toDto(Property property);

    @Mapping(target = "medias", expression = "java(mapMedia(property.getMedias()))")
    @Mapping(target = "location", source = "shortAddress")
    @Mapping(target = "amenities", source = "property.amenities")
    @Mapping(target = "latitude", source = "property.location.latitude")
    @Mapping(target = "longitude", source = "property.location.longitude")
    @Mapping(target = "ownerContact", source = "property.ownerOrAgentContact")
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
