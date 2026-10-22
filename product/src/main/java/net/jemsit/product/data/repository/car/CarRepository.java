package net.jemsit.product.data.repository.car;

import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.dto.response.product.car.CarsStats;
import net.jemsit.product.data.model.car.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {

    @Query(value = """
            SELECT CASE WHEN :lang = 'ru' THEN COALESCE(NULLIF(r.name_ru, ''), r.name_uz) ELSE COALESCE(NULLIF(r.name_uz, ''), r.name_ru) END
            FROM cars c LEFT JOIN regions r ON r.id = c.region_id WHERE c.id = :carId

            UNION ALL

            SELECT CASE WHEN :lang = 'ru' THEN COALESCE(NULLIF(d.name_ru, ''), d.name_uz) ELSE COALESCE(NULLIF(d.name_uz, ''), d.name_ru) END
            FROM cars c LEFT JOIN districts d ON d.id = c.district_id WHERE c.id = :carId

            UNION ALL

            SELECT CASE WHEN :lang = 'ru' THEN COALESCE(NULLIF(v.name_ru, ''), v.name_uz) ELSE COALESCE(NULLIF(v.name_uz, ''), v.name_ru) END
            FROM cars c LEFT JOIN villages v ON v.id = c.place_id WHERE c.id = :carId

            UNION ALL
            SELECT c.address FROM cars c WHERE c.id = :carId
            """, nativeQuery = true)
    List<String> findShortAddressById(@Param("carId") long carId, @Param("lang") String lang);

    Page<Car> findByAgentID(Long agentId, Pageable pageable);

    long countByListingStatus(ListingStatus listingStatus);

    @Query("SELECT new net.jemsit.common.dto.response.product.car.CarsStats(" +
            "COUNT(c), " +
            "COALESCE(SUM(CASE WHEN c.listingStatus = 'ACTIVE' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN c.occupancyStatus = 'SOLD' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN c.listingStatus = 'DRAFT' THEN 1 ELSE 0 END), 0)) " +
            "FROM Car c WHERE (:agentId IS NULL OR c.agentID = :agentId)")
    CarsStats getCarsStats(@Param("agentId") Long agentId);

    @Modifying
    @Query("UPDATE Car c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT c.viewCount FROM Car c WHERE c.id = :id")
    long findViewCountById(@Param("id") Long id);

    // Scalar projection, not findById: this is called from the public, unauthenticated view counter,
    // which only needs a name for the operator card. A car's name is its brand and model; the
    // free-text title is the fallback for rows that carry neither.
    @Query("SELECT c.brand, c.model, c.title FROM Car c WHERE c.id = :id")
    Object[] findNamePartsById(@Param("id") Long id);
}
