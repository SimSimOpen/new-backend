package net.jemsit.product.mapper;

import net.jemsit.common.dto.request.product.secondhand.SecondHandRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyMedia;
import net.jemsit.common.dto.response.product.secondhand.SecondHandResponseDTO;
import net.jemsit.product.data.model.secondhand.SecondHandItem;
import net.jemsit.product.data.model.secondhand.SecondHandMediaData;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class SecondHandMapper {

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Autowired
    protected AgentPhones agentPhones;

    // Partial update: drafts are saved field by field, so a null in the request means "unchanged".
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "ownerOrAgentContact", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "agentID", ignore = true)
    @Mapping(target = "medias", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    public abstract void update(SecondHandRequestDTO request, @MappingTarget SecondHandItem item);

    // The phone on a listing is always the current one of the agent who owns it.
    @Mapping(target = "ownerContact", expression = "java(agentPhones.of(item.getAgentID()))")
    @Mapping(target = "medias", expression = "java(mapMedia(item.getMedias()))")
    @Mapping(target = "location", source = "shortAddress")
    public abstract SecondHandResponseDTO toDto(SecondHandItem item, List<String> shortAddress);

    public List<PropertyMedia> mapMedia(List<SecondHandMediaData> media) {
        if (media == null) {
            return null;
        }
        return media.stream()
                .map(m -> new PropertyMedia(m.getId(), minioEndpoint + m.getMediaURL(), m.getIsCoverImage()))
                .toList();
    }
}
