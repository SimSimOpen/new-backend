package net.jemsit.product.data.repository.services;

import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.dto.response.product.services.ServiceListingStats;
import net.jemsit.product.data.model.services.ServiceListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceListingRepository extends JpaRepository<ServiceListing, Long>, JpaSpecificationExecutor<ServiceListing> {

    @Query(value = """
            SELECT CASE WHEN :lang = 'ru' THEN COALESCE(NULLIF(r.name_ru, ''), r.name_uz) ELSE COALESCE(NULLIF(r.name_uz, ''), r.name_ru) END
            FROM service_listings s LEFT JOIN regions r ON r.id = s.region_id WHERE s.id = :listingId

            UNION ALL

            SELECT CASE WHEN :lang = 'ru' THEN COALESCE(NULLIF(d.name_ru, ''), d.name_uz) ELSE COALESCE(NULLIF(d.name_uz, ''), d.name_ru) END
            FROM service_listings s LEFT JOIN districts d ON d.id = s.district_id WHERE s.id = :listingId

            UNION ALL

            SELECT CASE WHEN :lang = 'ru' THEN COALESCE(NULLIF(v.name_ru, ''), v.name_uz) ELSE COALESCE(NULLIF(v.name_uz, ''), v.name_ru) END
            FROM service_listings s LEFT JOIN villages v ON v.id = s.place_id WHERE s.id = :listingId

            UNION ALL
            SELECT s.address FROM service_listings s WHERE s.id = :listingId
            """, nativeQuery = true)
    List<String> findShortAddressById(@Param("listingId") long listingId, @Param("lang") String lang);

    Page<ServiceListing> findByAgentID(Long agentId, Pageable pageable);

    long countByListingStatus(ListingStatus listingStatus);

    @Query("SELECT new net.jemsit.common.dto.response.product.services.ServiceListingStats(" +
            "COUNT(s), " +
            "COALESCE(SUM(CASE WHEN s.listingStatus = 'ACTIVE' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN s.listingStatus = 'DRAFT' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN s.kind = 'OFFERING' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN s.kind = 'SEEKING' THEN 1 ELSE 0 END), 0)) " +
            "FROM ServiceListing s WHERE (:agentId IS NULL OR s.agentID = :agentId)")
    ServiceListingStats getServiceListingStats(@Param("agentId") Long agentId);

    @Modifying
    @Query("UPDATE ServiceListing s SET s.viewCount = s.viewCount + 1 WHERE s.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT s.viewCount FROM ServiceListing s WHERE s.id = :id")
    long findViewCountById(@Param("id") Long id);

    // Scalar title for the operator card on the public, unauthenticated view counter. A single
    // String return (not Object[]) so Spring Data maps the one row directly, not as a collection.
    @Query("SELECT s.title FROM ServiceListing s WHERE s.id = :id")
    String findTitleById(@Param("id") Long id);
}
