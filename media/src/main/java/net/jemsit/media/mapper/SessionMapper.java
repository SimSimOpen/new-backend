package net.jemsit.media.mapper;

import net.jemsit.common.dto.response.media.SessionResponseDTO;
import net.jemsit.media.data.model.Session;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SessionMapper {

    SessionResponseDTO toDto(Session session);
}
