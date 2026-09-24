package net.jemsit.media.service.impl;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.api.ProductApi;
import net.jemsit.common.dto.message.UserAvatarUpdated;
import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.media.service.AsyncMediaService;
import net.jemsit.media.service.FileData;
import net.jemsit.media.service.ImageProcessingService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncMediaServiceImpl implements AsyncMediaService {

    private static final String PROPERTIES_BASE_PATH = "properties/";
    private static final String USER_AVATARS_BASE_PATH = "user-avatars/";
    private static final String PRODUCT_BUCKET_NAME = "real-estate-media";
    private static final String USER_BUCKET_NAME = "user-media";

    private final MinioClient minioClient;
    private final ProductApi productApi;
    private final ImageProcessingService imageProcessingService;
    private final ApplicationEventPublisher eventPublisher;
    private final Semaphore ffmpegSemaphore;

    @Override
    @Async
    public void asyncProductMediaUpload(Long propertyId, List<FileData> files, Long userId) {
        try {
            List<String> urls = processFilesWithBoundedConcurrency(propertyId, files);

            if (urls.isEmpty()) {
                log.error("All file uploads failed for property {}", propertyId);
                return;
            }
            if (urls.size() < files.size()) {
                log.warn("Partial upload for property {}: {}/{} files succeeded",
                        propertyId, urls.size(), files.size());
            }

            productApi.addPropertyImage(new AddPropertyImagesRequestDTO(propertyId, urls), userId);
        } catch (Exception e) {
            log.error("Fatal error during media upload for property {}: {}", propertyId, e.getMessage(), e);
        }
    }

    @Override
    public void asyncUserAvatarUpload(Long userId, FileData file) {
        try {
            String url = processAndUpload(USER_AVATARS_BASE_PATH, USER_BUCKET_NAME,
                    file.bytes(), file.originalFileName(), userId);
            eventPublisher.publishEvent(new UserAvatarUpdated(String.valueOf(userId), url));
        } catch (Exception e) {
            log.error("Error processing and uploading user avatar: {}", e.getMessage());
        }
    }

    private List<String> processFilesWithBoundedConcurrency(Long propertyId, List<FileData> files) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<String>> futures = files.stream()
                    .map(file -> CompletableFuture.supplyAsync(
                            () -> processWithSemaphore(file, propertyId), executor))
                    .toList();

            return futures.stream()
                    .map(f -> f.exceptionally(ex -> {
                        log.error("Upload task failed: {}", ex.getMessage(),ex);
                        return null;
                    }))
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private String processWithSemaphore(FileData file, Long propertyId) {
        try {
            ffmpegSemaphore.acquire();
            try {
                return processAndUpload(PROPERTIES_BASE_PATH, PRODUCT_BUCKET_NAME,
                        file.bytes(), file.originalFileName(), propertyId);
            } finally {
                ffmpegSemaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for FFmpeg semaphore", e);
        } catch (Exception e) {
            throw new IllegalStateException("Media processing failed for " + file.originalFileName(), e);
        }
    }

    private String processAndUpload(String basePath, String bucketName, byte[] bytes, String filename, Long id)
            throws Exception {
        byte[] processedBytes = imageProcessingService.processImageWithWaterMark(bytes, filename);
        String objectKey = basePath + id + "/images/" + UUID.randomUUID() + ".webp";

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .contentType("image/webp")
                        .stream(new ByteArrayInputStream(processedBytes), processedBytes.length, -1)
                        .build()
        );

        return "/" + bucketName + "/" + objectKey;
    }
}
