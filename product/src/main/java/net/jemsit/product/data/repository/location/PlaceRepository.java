package net.jemsit.product.data.repository.location;

import net.jemsit.product.data.model.location.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    List<Place> findByDistrictId(Long districtId);
}
