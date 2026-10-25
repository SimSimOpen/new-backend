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
    private static final String CARS_BASE_PATH = "cars/";
    private static final String SECOND_HAND_BASE_PATH = "second-hand/";
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
        List<String> urls = uploadListingMedia(PROPERTIES_BASE_PATH, propertyId, files);
        if (!urls.isEmpty()) {
            productApi.addPropertyImage(new AddPropertyImagesRequestDTO(propertyId, urls), userId);
        }
    }

    @Override
    @Async
    public void asyncCarMediaUpload(Long carId, List<FileData> files, Long userId) {
        List<String> urls = uploadListingMedia(CARS_BASE_PATH, carId, files);
        if (!urls.isEmpty()) {
            productApi.addCarImage(new AddPropertyImagesRequestDTO(carId, urls), userId);
        }
    }

    @Override
    @Async
    public void asyncSecondHandMediaUpload(Long itemId, List<FileData> files, Long userId) {
        List<String> urls = uploadListingMedia(SECOND_HAND_BASE_PATH, itemId, files);
        if (!urls.isEmpty()) {
            productApi.addSecondHandImage(new AddPropertyImagesRequestDTO(itemId, urls), userId);
        }
    }

    /** Uploads a listing's photos; returns the stored URLs, empty when nothing made it. */
    private List<String> uploadListingMedia(String basePath, Long listingId, List<FileData> files) {
        try {
            List<String> urls = processFilesWithBoundedConcurrency(basePath, listingId, files);
            if (urls.isEmpty()) {
                log.error("All file uploads failed for {}{}", basePath, listingId);
            } else if (urls.size() < files.size()) {
                log.warn("Partial upload for {}{}: {}/{} files succeeded",
                        basePath, listingId, urls.size(), files.size());
            }
            return urls;
        } catch (Exception e) {
            log.error("Fatal error during media upload for {}{}: {}", basePath, listingId, e.getMessage(), e);
            return List.of();
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

    private List<String> processFilesWithBoundedConcurrency(String basePath, Long listingId, List<FileData> files) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<String>> futures = files.stream()
                    .map(file -> CompletableFuture.supplyAsync(
                            () -> processWithSemaphore(basePath, file, listingId), executor))
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

    private String processWithSemaphore(String basePath, FileData file, Long listingId) {
        try {
            ffmpegSemaphore.acquire();
            try {
                return processAndUpload(basePath, PRODUCT_BUCKET_NAME,
                        file.bytes(), file.originalFileName(), listingId);
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
