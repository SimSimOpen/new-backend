package net.jemsit.common.clients.media;

import net.jemsit.common.clients.FeignAuthInterceptor;
import net.jemsit.common.dto.response.media.UploadImagesResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "media-service", path = "/api/media", configuration = FeignAuthInterceptor.class)
public interface MediaServiceClient {
    @PostMapping("/v1/upload-product-media")
    UploadImagesResponseDTO uploadMedia(@RequestParam(value = "property_id", required = false) Long property_id, @RequestPart("files") List<MultipartFile> files);

    @DeleteMapping("/v1/delete/media")
    void deleteMedia(@RequestParam("media-url") String mediaUrl);
}
