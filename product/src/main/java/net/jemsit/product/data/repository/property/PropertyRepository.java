package net.jemsit.product.data.repository.property;

import net.jemsit.common.dto.response.product.propeprty.PropertiesStats;
import net.jemsit.product.data.model.property.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    @Query(value = """
            SELECT r.name_ru
            FROM property_locations pl
            LEFT JOIN regions r ON r.id = pl.region_id
            WHERE pl.id = :locationId

            UNION ALL

            SELECT d.name_ru
            FROM property_locations pl
            LEFT JOIN districts d ON d.id = pl.district_id
            WHERE pl.id = :locationId

            UNION ALL

            SELECT p.name_ru
            FROM property_locations pl
            LEFT JOIN villages p ON p.id = pl.place_id
            WHERE pl.id = :locationId

            UNION ALL
            SELECT pl.address
            FROM property_locations pl
            WHERE pl.id = :locationId
            """, nativeQuery = true)
    List<String> findShortAddressById(long locationId);

    Page<Property> findByAgentID(Long id, Pageable pageable);

    @Query(value = "SELECT * FROM properties WHERE listing_status = 'ACTIVE'", nativeQuery = true)
    Page<Property> findAllPublished(Pageable pageable);


    @Query("SELECT  new net.jemsit.common.dto.response.product.propeprty.PropertiesStats(" +
            "COUNT(p), " +
            "SUM(CASE WHEN p.listingStatus = 'ACTIVE' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN p.occupancyStatus = 'RENTED' THEN 1 ELSE 0 END)," +
            "SUM(CASE WHEN p.listingStatus = 'DRAFT' THEN 1 ELSE 0 END)) " +
            "FROM Property p")
    PropertiesStats getPropertiesStats();

    @Query("SELECT new net.jemsit.common.dto.response.product.propeprty.PropertiesStats(" +
            "COUNT(p), " +
            "COALESCE(SUM(CASE WHEN p.listingStatus = 'ACTIVE' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN p.occupancyStatus = 'RENTED' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN p.listingStatus = 'DRAFT' THEN 1 ELSE 0 END), 0)) " +
            "FROM Property p WHERE p.agentID = :agentId")
    PropertiesStats getPropertiesStatsByAgentId(@Param("agentId") Long agentId);


    @Query(value = """
        SELECT p.* FROM properties p
        JOIN property_locations l ON p.location_id = l.id
        LEFT JOIN regions r ON r.id = l.region_id
        LEFT JOIN districts d ON d.id = l.district_id
        LEFT JOIN villages v ON v.id = l.place_id
        WHERE
            to_tsvector('simple',
                COALESCE(p.title, '') || ' ' ||
                COALESCE(p.description, '') || ' ' ||
                COALESCE(l.address, '') || ' ' ||
                COALESCE(r.name_ru, '') || ' ' ||
                COALESCE(r.name_uz, '') || ' ' ||
                COALESCE(d.name_ru, '') || ' ' ||
                COALESCE(d.name_uz, '') || ' ' ||
                COALESCE(v.name_ru, '') || ' ' ||
                COALESCE(v.name_uz, '')
            ) @@ plainto_tsquery('simple', :search)
            AND (:listingStatus IS NULL OR p.listing_status = :listingStatus)
            AND (:type IS NULL OR p.type = :type)
            AND (:category IS NULL OR p.category = :category)
            AND (:offerType IS NULL OR p.offer_type = :offerType)
            AND (:occupancyStatus IS NULL OR p.occupancy_status = :occupancyStatus)
            AND (:agentID IS NULL OR p.agentID = :agentID)
        """, countQuery = """
        SELECT COUNT(p.id) FROM properties p
        JOIN property_locations l ON p.location_id = l.id
        LEFT JOIN regions r ON r.id = l.region_id
        LEFT JOIN districts d ON d.id = l.district_id
        LEFT JOIN villages v ON v.id = l.place_id
        WHERE
            to_tsvector('simple',
                COALESCE(p.title, '') || ' ' ||
                COALESCE(p.description, '') || ' ' ||
                COALESCE(l.address, '') || ' ' ||
                COALESCE(r.name_ru, '') || ' ' ||
                COALESCE(r.name_uz, '') || ' ' ||
                COALESCE(d.name_ru, '') || ' ' ||
                COALESCE(d.name_uz, '') || ' ' ||
                COALESCE(v.name_ru, '') || ' ' ||
                COALESCE(v.name_uz, '')
            ) @@ plainto_tsquery('simple', :search)
            AND (:listingStatus IS NULL OR p.listing_status = :listingStatus)
            AND (:type IS NULL OR p.type = :type)
            AND (:category IS NULL OR p.category = :category)
            AND (:offerType IS NULL OR p.offer_type = :offerType)
            AND (:occupancyStatus IS NULL OR p.occupancy_status = :occupancyStatus)
            AND (:agentID IS NULL OR p.agentID = :agentID)
        """, nativeQuery = true)
    Page<Property> search(
            @Param("agentID") Long agentID,
            @Param("search") String search,
            @Param("listingStatus") String listingStatus,
            @Param("type") String type,
            @Param("category") String category,
            @Param("offerType") String offerType,
            @Param("occupancyStatus") String occupancyStatus,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Property p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT p.viewCount FROM Property p WHERE p.id = :id")
    long findViewCountByPropertyId(@Param("id") Long id);
}
