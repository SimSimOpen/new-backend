package net.jemsit.product.data.repository.property;

import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.dto.response.product.propeprty.PropertiesStats;
import net.jemsit.product.data.model.property.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    @Query(value = """
            SELECT CASE WHEN :lang = 'ru' THEN COALESCE(NULLIF(r.name_ru, ''), r.name_uz) ELSE COALESCE(NULLIF(r.name_uz, ''), r.name_ru) END
            FROM property_locations pl
            LEFT JOIN regions r ON r.id = pl.region_id
            WHERE pl.id = :locationId

            UNION ALL

            SELECT CASE WHEN :lang = 'ru' THEN COALESCE(NULLIF(d.name_ru, ''), d.name_uz) ELSE COALESCE(NULLIF(d.name_uz, ''), d.name_ru) END
            FROM property_locations pl
            LEFT JOIN districts d ON d.id = pl.district_id
            WHERE pl.id = :locationId

            UNION ALL

            SELECT CASE WHEN :lang = 'ru' THEN COALESCE(NULLIF(p.name_ru, ''), p.name_uz) ELSE COALESCE(NULLIF(p.name_uz, ''), p.name_ru) END
            FROM property_locations pl
            LEFT JOIN villages p ON p.id = pl.place_id
            WHERE pl.id = :locationId

            UNION ALL
            SELECT pl.address
            FROM property_locations pl
            WHERE pl.id = :locationId
            """, nativeQuery = true)
    List<String> findShortAddressById(@Param("locationId") long locationId, @Param("lang") String lang);

    Page<Property> findByAgentID(Long id, Pageable pageable);

    long countByListingStatus(ListingStatus listingStatus);

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
        LEFT JOIN property_amenities am ON am.id = p.amenities_id
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
            AND (CAST(:regionId AS bigint) IS NULL OR l.region_id = :regionId)
            AND (CAST(:districtId AS bigint) IS NULL OR l.district_id = :districtId)
            AND (CAST(:placeId AS bigint) IS NULL OR l.place_id = :placeId)
            AND (:typeFilter IS NULL OR p.type = ANY (string_to_array(:typeFilter, ',')))
            AND (CAST(:priceFrom AS numeric) IS NULL OR p.price >= :priceFrom)
            AND (CAST(:priceTo AS numeric) IS NULL OR p.price <= :priceTo)
            AND (CAST(:roomsFrom AS integer) IS NULL OR p.number_of_rooms >= :roomsFrom)
            AND (CAST(:floorFrom AS integer) IS NULL OR p.floor >= :floorFrom)
            AND (CAST(:amenityParking AS boolean) IS NULL OR am.has_parking = true)
            AND (CAST(:amenitySwimmingPool AS boolean) IS NULL OR am.has_swimming_pool = true)
            AND (CAST(:amenityGym AS boolean) IS NULL OR am.has_gym = true)
            AND (CAST(:amenityAirConditioning AS boolean) IS NULL OR am.has_air_conditioning = true)
            AND (CAST(:amenityWashingMachine AS boolean) IS NULL OR am.has_washing_machine = true)
        ORDER BY
            CASE WHEN :sortBy = 'price_asc' THEN p.price END ASC NULLS LAST,
            CASE WHEN :sortBy = 'price_desc' THEN p.price END DESC NULLS LAST,
            CASE WHEN :sortBy = 'created_desc' THEN p.created_at END DESC NULLS LAST
        """, countQuery = """
        SELECT COUNT(p.id) FROM properties p
        JOIN property_locations l ON p.location_id = l.id
        LEFT JOIN regions r ON r.id = l.region_id
        LEFT JOIN districts d ON d.id = l.district_id
        LEFT JOIN villages v ON v.id = l.place_id
        LEFT JOIN property_amenities am ON am.id = p.amenities_id
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
            AND (CAST(:regionId AS bigint) IS NULL OR l.region_id = :regionId)
            AND (CAST(:districtId AS bigint) IS NULL OR l.district_id = :districtId)
            AND (CAST(:placeId AS bigint) IS NULL OR l.place_id = :placeId)
            AND (:typeFilter IS NULL OR p.type = ANY (string_to_array(:typeFilter, ',')))
            AND (CAST(:priceFrom AS numeric) IS NULL OR p.price >= :priceFrom)
            AND (CAST(:priceTo AS numeric) IS NULL OR p.price <= :priceTo)
            AND (CAST(:roomsFrom AS integer) IS NULL OR p.number_of_rooms >= :roomsFrom)
            AND (CAST(:floorFrom AS integer) IS NULL OR p.floor >= :floorFrom)
            AND (CAST(:amenityParking AS boolean) IS NULL OR am.has_parking = true)
            AND (CAST(:amenitySwimmingPool AS boolean) IS NULL OR am.has_swimming_pool = true)
            AND (CAST(:amenityGym AS boolean) IS NULL OR am.has_gym = true)
            AND (CAST(:amenityAirConditioning AS boolean) IS NULL OR am.has_air_conditioning = true)
            AND (CAST(:amenityWashingMachine AS boolean) IS NULL OR am.has_washing_machine = true)
        """, nativeQuery = true)
    Page<Property> search(
            @Param("agentID") Long agentID,
            @Param("search") String search,
            @Param("listingStatus") String listingStatus,
            @Param("type") String type,
            @Param("category") String category,
            @Param("offerType") String offerType,
            @Param("occupancyStatus") String occupancyStatus,
            @Param("regionId") Long regionId,
            @Param("districtId") Long districtId,
            @Param("placeId") Long placeId,
            @Param("typeFilter") String typeFilter,
            @Param("priceFrom") BigDecimal priceFrom,
            @Param("priceTo") BigDecimal priceTo,
            @Param("roomsFrom") Integer roomsFrom,
            @Param("floorFrom") Integer floorFrom,
            @Param("amenityParking") Boolean amenityParking,
            @Param("amenitySwimmingPool") Boolean amenitySwimmingPool,
            @Param("amenityGym") Boolean amenityGym,
            @Param("amenityAirConditioning") Boolean amenityAirConditioning,
            @Param("amenityWashingMachine") Boolean amenityWashingMachine,
            @Param("sortBy") String sortBy,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Property p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT p.viewCount FROM Property p WHERE p.id = :id")
    long findViewCountByPropertyId(@Param("id") Long id);

    // Scalar projection, not findById: this is called from the public, unauthenticated view counter,
    // and Property's @OneToOne location and amenities are EAGER, so loading the entity would put two
    // extra joins on a route that only needs a name.
    @Query("SELECT p.title FROM Property p WHERE p.id = :id")
    String findTitleById(@Param("id") Long id);
}
