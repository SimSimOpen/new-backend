package net.jemsit.product.service;

import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.request.product.secondhand.SecondHandFilterRequestDTO;
import net.jemsit.common.dto.request.product.secondhand.SecondHandRequestDTO;
import net.jemsit.common.dto.response.product.secondhand.SecondHandResponseDTO;
import net.jemsit.common.dto.response.product.secondhand.SecondHandStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SecondHandService {
    SecondHandResponseDTO createDraft(Long userId);

    SecondHandResponseDTO update(Long id, SecondHandRequestDTO request);

    SecondHandResponseDTO getById(Long id);

    String deleteById(Long id);

    void requireEditable(Long id);

    SecondHandResponseDTO addImages(AddPropertyImagesRequestDTO request, Long userId);

    void deleteImage(Long id, Long userId);

    Integer getMediaCount(Long itemId);

    Page<SecondHandResponseDTO> getAgentsAll(Pageable pageable);

    Page<SecondHandResponseDTO> filter(SecondHandFilterRequestDTO filter, Pageable pageable);

    SecondHandStats getStats();

    long incrementViews(Long id);
}
