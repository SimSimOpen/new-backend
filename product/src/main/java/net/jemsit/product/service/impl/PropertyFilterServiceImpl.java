package net.jemsit.product.service.impl;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.PropertyType;
import net.jemsit.common.dto.request.product.property.PropertyFilterRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import net.jemsit.product.data.model.property.Property;
import net.jemsit.product.data.model.property.PropertyLocation;
import net.jemsit.product.data.repository.property.PropertyRepository;
import net.jemsit.product.mapper.LocationNames;
import net.jemsit.product.mapper.PropertyMapper;
import net.jemsit.product.service.PropertyFilterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyFilterServiceImpl implements PropertyFilterService {

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;

    @Override
    public Page<PropertyResponseDTO> filterProperties(PropertyFilterRequestDTO filterRequest, Pageable pageable) {
        boolean mine = Boolean.TRUE.equals(filterRequest.mine());
        Long agentScope = agentScopedId(mine);
        // Same rule as cars and second-hand: the panel (`mine`) may ask for any status, the public
        // browse only ever sees published listings, whatever the caller sends or carries.
        ListingStatus status = mine ? filterRequest.listingStatus() : ListingStatus.ACTIVE;
        List<PropertyType> types = filterRequest.types() == null ? List.of() : filterRequest.types();
        List<String> amenities = filterRequest.amenities() == null ? List.of() : filterRequest.amenities();

        if (filterRequest.search() != null && !filterRequest.search().isBlank()) {
            Page<Property> result = propertyRepository.search(
                    agentScope,
                    filterRequest.search(),
                    nameOf(status),
                    nameOf(filterRequest.type()),
                    nameOf(filterRequest.category()),
                    nameOf(filterRequest.offerType()),
                    nameOf(filterRequest.occupancyStatus()),
                    filterRequest.regionId(),
                    filterRequest.districtId(),
                    filterRequest.placeId(),
                    typeFilterOf(types),
                    filterRequest.priceFrom(),
                    filterRequest.priceTo(),
                    filterRequest.roomsFrom(),
                    filterRequest.floorFrom(),
                    amenityFlag(amenities, "parking"),
                    amenityFlag(amenities, "swimmingPool"),
                    amenityFlag(amenities, "gym"),
                    amenityFlag(amenities, "airConditioning"),
                    amenityFlag(amenities, "washingMachine"),
                    nativeSortKey(pageable.getSort()),
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
            );
            return toDtoPage(result);
        }

        Specification<Property> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (agentScope != null) {
                predicates.add(criteriaBuilder.equal(root.get("agentID"), agentScope));
            }
            if (filterRequest.category() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), filterRequest.category()));
            }
            if (filterRequest.type() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), filterRequest.type()));
            }
            if (!types.isEmpty()) {
                predicates.add(root.get("type").in(types));
            }
            if (filterRequest.offerType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("offerType"), filterRequest.offerType()));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("listingStatus"), status));
            }
            if (filterRequest.occupancyStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("occupancyStatus"), filterRequest.occupancyStatus()));
            }
            if (filterRequest.regionId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("location").get("regionID"), filterRequest.regionId()));
            }
            if (filterRequest.districtId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("location").get("districtID"), filterRequest.districtId()));
            }
            if (filterRequest.placeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("location").get("placeID"), filterRequest.placeId()));
            }
            if (filterRequest.priceFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filterRequest.priceFrom()));
            }
            if (filterRequest.priceTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filterRequest.priceTo()));
            }
            if (filterRequest.roomsFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("numberOfRooms"), filterRequest.roomsFrom()));
            }
            if (filterRequest.floorFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("floor"), filterRequest.floorFrom()));
            }
            if (!amenities.isEmpty()) {
                var amenitiesJoin = root.join("amenities", JoinType.INNER);
                for (String amenity : amenities) {
                    String attribute = amenityAttribute(amenity);
                    if (attribute != null) {
                        predicates.add(criteriaBuilder.isTrue(amenitiesJoin.get(attribute)));
                    }
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return toDtoPage(propertyRepository.findAll(specification, pageable));
    }

    public List<String> getLocationList(PropertyLocation location) {
        if (location == null) {
            return List.of("", "", "");
        }
        // Refs cleared by a dataset migration resolve to null; keep the slots so the address stays
        // at index 3, but hand clients blanks rather than nulls they would render as "null".
        return propertyRepository.findShortAddressById(location.getId(), LocationNames.currentLang()).stream()
                .map(part -> part == null ? "" : part)
                .toList();
    }

    private Page<PropertyResponseDTO> toDtoPage(Page<Property> properties) {
        return properties.map(property ->
                propertyMapper.toDtoWithShortAddress(property, getLocationList(property.getLocation())));
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }

    /** Translates the requested sort into the key the native query's CASE ORDER BY understands. */
    private static String nativeSortKey(Sort sort) {
        return sort.stream().findFirst().map(order -> {
            if ("price".equals(order.getProperty())) {
                return order.isAscending() ? "price_asc" : "price_desc";
            }
            return "createdAt".equals(order.getProperty()) && order.isDescending() ? "created_desc" : null;
        }).orElse(null);
    }

    private static String typeFilterOf(List<PropertyType> types) {
        return types.isEmpty() ? null : String.join(",", types.stream().map(Enum::name).toList());
    }

    private static Boolean amenityFlag(List<String> amenities, String key) {
        return amenities.contains(key) ? Boolean.TRUE : null;
    }

    /** Maps a client amenity key to the entity flag; unknown keys are ignored, not an error. */
    private static String amenityAttribute(String key) {
        return switch (key) {
            case "parking" -> "hasParking";
            case "garden" -> "hasGarden";
            case "swimmingPool" -> "hasSwimmingPool";
            case "gym" -> "hasGym";
            case "security" -> "hasSecurity";
            case "elevator" -> "hasElevator";
            case "washingMachine" -> "hasWashingMachine";
            case "airConditioning" -> "hasAirConditioning";
            case "internet" -> "hasInternet";
            case "refrigerator" -> "hasRefrigerator";
            case "dishwasher" -> "hasDishwasher";
            case "microwave" -> "hasMicrowave";
            case "parkingSpace" -> "hasParkingSpace";
            case "tv" -> "hasTV";
            case "satellite" -> "hasSatellite";
            case "furniture" -> "hasFurniture";
            default -> null;
        };
    }

    /**
     * The admin panel sends `mine=true` when a signed-in agent asks for his own listings. The client
     * browse never does, so an agent looking at the site sees exactly what a visitor sees: without
     * that flag the caller's identity is ignored here.
     */
    private static Long agentScopedId(boolean mine) {
        if (!mine) {
            return null;
        }
        List<Roles> roles = UserContext.getRoles();
        boolean isAgent = roles != null && roles.contains(Roles.AGENT) && !roles.contains(Roles.ADMIN);
        return isAgent ? UserContext.getUserId() : null;
    }
}
