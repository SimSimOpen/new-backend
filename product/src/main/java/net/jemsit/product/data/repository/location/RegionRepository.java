package net.jemsit.product.data.repository.location;

import net.jemsit.product.data.model.location.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {
}
