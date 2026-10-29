package net.jemsit.product.mapper;

import net.jemsit.common.dto.request.product.services.ServiceListingRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyMedia;
import net.jemsit.common.dto.response.product.services.ServiceListingResponseDTO;
import net.jemsit.product.data.model.services.ServiceListing;
import net.jemsit.product.data.model.services.ServiceListingMedia;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ServiceListingMapper {

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    // Partial update: drafts are saved field by field, so a null in the request means "unchanged".
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "ownerOrAgentContact", source = "ownerContact")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "agentID", ignore = true)
    @Mapping(target = "medias", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    public abstract void update(ServiceListingRequestDTO request, @MappingTarget ServiceListing listing);

    @Mapping(target = "ownerContact", source = "listing.ownerOrAgentContact")
    @Mapping(target = "medias", expression = "java(mapMedia(listing.getMedias()))")
    @Mapping(target = "location", source = "shortAddress")
    public abstract ServiceListingResponseDTO toDto(ServiceListing listing, List<String> shortAddress);

    public List<PropertyMedia> mapMedia(List<ServiceListingMedia> media) {
        if (media == null) {
            return null;
        }
        return media.stream()
                .map(m -> new PropertyMedia(m.getId(), minioEndpoint + m.getMediaURL(), m.getIsCoverImage()))
                .toList();
    }
}
