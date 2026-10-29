package net.jemsit.media.controller;

import lombok.RequiredArgsConstructor;
import net.jemsit.media.service.MediaService;
import net.jemsit.media.service.SessionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/media/v1")
public class MediaController {

    private final MediaService mediaService;
    private final SessionService sessionService;

    @PostMapping(value = "/upload-product-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMedia(@RequestParam(value = "property_id", required = false) Long propertyId,
                                         @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(mediaService.uploadMedia(propertyId, files));
    }

    @PostMapping(value = "/upload-car-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCarMedia(@RequestParam(value = "car_id", required = false) Long carId,
                                            @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(mediaService.uploadCarMedia(carId, files));
    }

    @PostMapping(value = "/upload-second-hand-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSecondHandMedia(@RequestParam(value = "item_id", required = false) Long itemId,
                                                   @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(mediaService.uploadSecondHandMedia(itemId, files));
    }

    @PostMapping(value = "/upload-service-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadServiceMedia(@RequestParam(value = "listing_id", required = false) Long listingId,
                                                @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(mediaService.uploadServiceMedia(listingId, files));
    }

    @PostMapping(value = "/upload-user-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadUserAvatar(@RequestParam("user_id") Long userId,
                                              @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mediaService.uploadUserAvatar(userId, file));
    }

    @DeleteMapping("/delete/media")
    public ResponseEntity<?> deleteMedia(@RequestParam("media-url") String mediaUrl) {
        mediaService.deleteMedia(mediaUrl);
        return ResponseEntity.ok("Media deleted successfully");
    }

    @PostMapping("/session")
    public ResponseEntity<?> createUploadSession() {
        return ResponseEntity.ok(sessionService.createUploadSession());
    }

    @GetMapping("/session/{sessionId}/status")
    public void getSession(@PathVariable String sessionId) {
        sessionService.getSession(sessionId);
    }
}
