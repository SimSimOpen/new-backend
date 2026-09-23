package net.jemsit.media.api;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.api.MediaApi;
import net.jemsit.media.service.MediaService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaApiImpl implements MediaApi {

    private final MediaService mediaService;

    @Override
    public void deleteMedia(String mediaUrl) {
        mediaService.deleteMedia(mediaUrl);
    }
}
