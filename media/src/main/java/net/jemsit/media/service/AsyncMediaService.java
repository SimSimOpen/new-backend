package net.jemsit.media.service;

import java.util.List;

public interface AsyncMediaService {
    void asyncProductMediaUpload(Long propertyId, List<FileData> files, Long userId);

    void asyncCarMediaUpload(Long carId, List<FileData> files, Long userId);

    void asyncSecondHandMediaUpload(Long itemId, List<FileData> files, Long userId);

    void asyncUserAvatarUpload(Long userId, FileData file);
}
