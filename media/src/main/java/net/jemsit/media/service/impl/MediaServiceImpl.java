package net.jemsit.media.service.impl;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.ProductApi;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.media.service.AsyncMediaService;
import net.jemsit.media.service.FileData;
import net.jemsit.media.service.MediaService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MinioClient minioClient;
    private final ProductApi productApi;
    private final AsyncMediaService asyncMediaService;

    @Override
    public Long uploadMedia(Long propertyId, List<MultipartFile> files) {
        Long userId = UserContext.getUserId();

        if (propertyId == null) {
            propertyId = productApi.createPropertyDraft(userId).id();
        }

        List<FileData> fileDataList = files.stream()
                .map(file -> {
                    try {
                        return new FileData(file.getBytes(), file.getOriginalFilename());
                    } catch (Exception e) {
                        log.error("Error reading file bytes: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        asyncMediaService.asyncProductMediaUpload(propertyId, fileDataList, userId);
        return propertyId;
    }

    @Override
    public void deleteMedia(String mediaUrl) {
        var parts = mediaUrl.split("/");
        String bucket = parts[1];
        String object = mediaUrl.substring(bucket.length() + 1);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(object)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error deleting file from MinIO: {}", e.getMessage());
            throw new UserException("Failed to delete image. Please try again.");
        }
    }

    @Override
    public Long uploadUserAvatar(Long userId, MultipartFile file) {
        try {
            asyncMediaService.asyncUserAvatarUpload(userId, new FileData(file.getBytes(), file.getOriginalFilename()));
            return userId;
        } catch (Exception e) {
            log.error("Error reading user avatar file bytes: {}", e.getMessage());
            throw new UserException("Failed to upload avatar. Please try again.");
        }
    }
}
