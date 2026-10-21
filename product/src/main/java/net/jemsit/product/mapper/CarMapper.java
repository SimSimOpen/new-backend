package net.jemsit.product.mapper;

import net.jemsit.common.dto.request.product.car.CarRequestDTO;
import net.jemsit.common.dto.response.product.car.CarResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyMedia;
import net.jemsit.product.data.model.car.Car;
import net.jemsit.product.data.model.car.CarMediaData;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class CarMapper {

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
    public abstract void update(CarRequestDTO request, @MappingTarget Car car);

    @Mapping(target = "ownerContact", source = "car.ownerOrAgentContact")
    @Mapping(target = "medias", expression = "java(mapMedia(car.getMedias()))")
    @Mapping(target = "location", source = "shortAddress")
    public abstract CarResponseDTO toDto(Car car, List<String> shortAddress);

    public List<PropertyMedia> mapMedia(List<CarMediaData> media) {
        if (media == null) {
            return null;
        }
        return media.stream()
                .map(m -> new PropertyMedia(m.getId(), minioEndpoint + m.getMediaURL(), m.getIsCoverImage()))
                .toList();
    }
}
