package net.jemsit.product.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.dto.request.product.property.PropertyFilterRequestDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import net.jemsit.product.data.model.property.Property;
import net.jemsit.product.data.model.property.PropertyLocation;
import net.jemsit.product.data.repository.property.PropertyRepository;
import net.jemsit.product.mapper.PropertyMapper;
import net.jemsit.product.service.PropertyFilterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        Long agentScope = agentScopedId();

        if (filterRequest.search() != null && !filterRequest.search().isBlank()) {
            Page<Property> result = propertyRepository.search(
                    agentScope,
                    filterRequest.search(),
                    nameOf(filterRequest.listingStatus()),
                    nameOf(filterRequest.type()),
                    nameOf(filterRequest.category()),
                    nameOf(filterRequest.offerType()),
                    nameOf(filterRequest.occupancyStatus()),
                    pageable
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
            if (filterRequest.offerType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("offerType"), filterRequest.offerType()));
            }
            if (filterRequest.listingStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("listingStatus"), filterRequest.listingStatus()));
            }
            if (filterRequest.occupancyStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("occupancyStatus"), filterRequest.occupancyStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return toDtoPage(propertyRepository.findAll(specification, pageable));
    }

    public List<String> getLocationList(PropertyLocation location) {
        if (location == null) {
            return List.of("", "", "");
        }
        return propertyRepository.findShortAddressById(location.getId());
    }

    private Page<PropertyResponseDTO> toDtoPage(Page<Property> properties) {
        return properties.map(property ->
                propertyMapper.toDtoWithShortAddress(property, getLocationList(property.getLocation())));
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static Long agentScopedId() {
        List<Roles> roles = UserContext.getRoles();
        boolean isAgent = roles != null && roles.contains(Roles.AGENT);
        return isAgent ? UserContext.getUserId() : null;
    }
}
