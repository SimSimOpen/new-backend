package net.jemsit.media.service;

import java.util.List;

public interface AsyncMediaService {
    void asyncProductMediaUpload(Long propertyId, List<FileData> files, Long userId);

    void asyncUserAvatarUpload(Long userId, FileData file);
}
