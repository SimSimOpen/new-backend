package net.jemsit.product.data.repository.location;

import net.jemsit.product.data.model.location.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByRegionId(Long regionId);
}
