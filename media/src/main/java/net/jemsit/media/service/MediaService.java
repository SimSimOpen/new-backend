package net.jemsit.media.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {
    Long uploadMedia(Long propertyId, List<MultipartFile> files);

    Long uploadCarMedia(Long carId, List<MultipartFile> files);

    Long uploadSecondHandMedia(Long itemId, List<MultipartFile> files);

    Long uploadServiceMedia(Long listingId, List<MultipartFile> files);

    void deleteMedia(String mediaUrl);

    Long uploadUserAvatar(Long userId, MultipartFile file);
}
