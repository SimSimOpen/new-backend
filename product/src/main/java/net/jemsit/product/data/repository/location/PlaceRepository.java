package net.jemsit.product.data.repository.location;

import net.jemsit.product.data.model.location.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    // Ids follow the quarter sequence within a district; sorting names puts "10-й квартал" before "2-й".
    List<Place> findByDistrictIdOrderByIdAsc(Long districtId);
}
