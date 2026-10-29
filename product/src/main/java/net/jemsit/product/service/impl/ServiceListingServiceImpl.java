package net.jemsit.product.service.impl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.api.MediaApi;
import net.jemsit.common.api.OperatorFeedApi;
import net.jemsit.common.data.enums.EventMessages;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.dto.message.MediaUploaded;
import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.request.product.services.ServiceListingFilterRequestDTO;
import net.jemsit.common.dto.request.product.services.ServiceListingRequestDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.dto.response.product.services.ServiceListingResponseDTO;
import net.jemsit.common.dto.response.product.services.ServiceListingStats;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.product.data.model.services.ServiceListing;
import net.jemsit.product.data.model.services.ServiceListingMedia;
import net.jemsit.product.data.repository.services.ServiceListingMediaRepository;
import net.jemsit.product.data.repository.services.ServiceListingRepository;
import net.jemsit.product.mapper.LocationNames;
import net.jemsit.product.mapper.ServiceListingMapper;
import net.jemsit.product.service.ServiceListingService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ServiceListingServiceImpl implements ServiceListingService {

    private final ServiceListingRepository listingRepository;
    private final ServiceListingMediaRepository mediaRepository;
    private final ServiceListingMapper mapper;
    // media calls back into product via ProductApi, so this edge stays lazy to avoid a startup cycle
    @Lazy
    private final MediaApi mediaApi;
    private final AuthApi authApi;
    private final OperatorFeedApi operatorFeedApi;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ServiceListingResponseDTO createDraft(Long userId) {
        ServiceListing listing = new ServiceListing();
        listing.setListingStatus(ListingStatus.DRAFT);
        UserDetailsResponseDTO userDetails = authApi.getById(userId);
        listing.setAgentID(userDetails.id());
        listing.setAgent(userDetails.username());
        return toDto(listingRepository.save(listing));
    }

    @Override
    @Transactional
    public ServiceListingResponseDTO update(Long id, ServiceListingRequestDTO request) {
        ServiceListing listing = requireOwnedListing(id);
        mapper.update(request, listing);
        return toDto(listingRepository.save(listing));
    }

    @Override
    public ServiceListingResponseDTO getById(Long id) {
        return toDto(requireVisibleListing(id));
    }

    @Override
    @Transactional
    public String deleteById(Long id) {
        ServiceListing listing = requireOwnedListing(id);
        for (ServiceListingMedia media : listing.getMedias()) {
            mediaApi.deleteMedia(media.getMediaURL());
        }
        listingRepository.delete(listing);
        return "Service listing with id " + id + " has been deleted successfully.";
    }

    @Override
    public void requireEditable(Long id) {
        requireOwnedListing(id);
    }

    // Called back from the async media upload, where UserContext is empty; ownership was checked on
    // the request thread through requireEditable before the upload was queued.
    @Override
    @Transactional
    public ServiceListingResponseDTO addImages(AddPropertyImagesRequestDTO request, Long userId) {
        ServiceListing listing = requireListing(request.id());
        boolean hasCoverImage = listing.getMedias().stream().anyMatch(ServiceListingMedia::getIsCoverImage);
        for (String url : request.urls()) {
            ServiceListingMedia image = new ServiceListingMedia();
            image.setMediaURL(url);
            image.setIsCoverImage(!hasCoverImage);
            hasCoverImage = true;
            listing.addMedia(image);
        }
        ServiceListing updated = listingRepository.save(listing);
        eventPublisher.publishEvent(new MediaUploaded(String.valueOf(userId), EventMessages.MEDIA_UPDATE));
        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteImage(Long id, Long userId) {
        ServiceListingMedia media = mediaRepository.findById(id)
                .orElseThrow(() -> new UserException("Service listing media not found with id: " + id));
        ServiceListing listing = requireOwnedListing(media.getListing().getId());

        mediaApi.deleteMedia(media.getMediaURL());
        listing.getMedias().remove(media);
        if (Boolean.TRUE.equals(media.getIsCoverImage()) && !listing.getMedias().isEmpty()) {
            listing.getMedias().getFirst().setIsCoverImage(true);
        }
        listingRepository.save(listing);
        eventPublisher.publishEvent(new MediaUploaded(String.valueOf(userId), EventMessages.MEDIA_UPDATE));
    }

    @Override
    public Integer getMediaCount(Long listingId) {
        return requireVisibleListing(listingId).getMedias().size();
    }

    @Override
    public Page<ServiceListingResponseDTO> getAgentsAll(Pageable pageable) {
        Page<ServiceListing> listings = hasRole(Roles.ADMIN)
                ? listingRepository.findAll(pageable)
                : listingRepository.findByAgentID(UserContext.getUserId(), pageable);
        return listings.map(this::toDto);
    }

    @Override
    public Page<ServiceListingResponseDTO> filter(ServiceListingFilterRequestDTO filter, Pageable pageable) {
        // Same rule as the other categories: with `mine` (the admin panel) agents see only their own
        // listings in any status and admins see everything. Without it the caller is the public,
        // whatever token they carry, so an agent browsing the client sees what a visitor sees.
        boolean mine = Boolean.TRUE.equals(filter.mine());
        boolean isAdmin = hasRole(Roles.ADMIN);
        boolean isAgent = !isAdmin && hasRole(Roles.AGENT);
        boolean staff = mine && (isAdmin || isAgent);
        ListingStatus status = staff ? filter.listingStatus() : ListingStatus.ACTIVE;
        Long agentScope = mine && isAgent ? UserContext.getUserId() : null;

        Specification<ServiceListing> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (agentScope != null) {
                predicates.add(cb.equal(root.get("agentID"), agentScope));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("listingStatus"), status));
            }
            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
            }
            // The intent toggle: OFFERING rows are suppliers, SEEKING rows are people looking for one.
            addEqual(predicates, cb, root.get("kind"), filter.kind());
            addIn(predicates, root.get("category"), filter.categories());
            addEqual(predicates, cb, root.get("regionId"), filter.regionId());
            addEqual(predicates, cb, root.get("districtId"), filter.districtId());
            addEqual(predicates, cb, root.get("placeId"), filter.placeId());
            if (filter.priceFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.priceFrom()));
            }
            if (filter.priceTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.priceTo()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return listingRepository.findAll(specification, pageable).map(this::toDto);
    }

    @Override
    public ServiceListingStats getStats() {
        return listingRepository.getServiceListingStats(hasRole(Roles.ADMIN) ? null : UserContext.getUserId());
    }

    @Override
    @Transactional
    public long incrementViews(Long id) {
        if (!listingRepository.existsById(id)) {
            throw new UserException("Service listing not found with id: " + id);
        }
        listingRepository.incrementViewCount(id);
        long views = listingRepository.findViewCountById(id);
        // Predicate first: most views are not milestones, and this is a public, unauthenticated route
        // that should not pay for a title lookup it will throw away.
        if (operatorFeedApi.isViewMilestone(views)) {
            operatorFeedApi.viewMilestone("services", id, views, listingRepository.findTitleById(id));
        }
        return views;
    }

    private ServiceListingResponseDTO toDto(ServiceListing listing) {
        // Refs cleared by a dataset migration resolve to null; hand clients blanks, not "null".
        List<String> shortAddress = listing.getId() == null ? List.of("", "", "", "")
                : listingRepository.findShortAddressById(listing.getId(), LocationNames.currentLang()).stream()
                .map(part -> part == null ? "" : part)
                .toList();
        return mapper.toDto(listing, shortAddress);
    }

    private ServiceListing requireListing(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new UserException("Service listing not found with id: " + id));
    }

    /** Live listings are public; drafts and archived ones only to their agent or an admin. */
    private ServiceListing requireVisibleListing(Long id) {
        ServiceListing listing = requireListing(id);
        if (listing.getListingStatus() != ListingStatus.ACTIVE && !canEdit(listing)) {
            throw new UserException("Service listing not found with id: " + id);
        }
        return listing;
    }

    private static boolean canEdit(ServiceListing listing) {
        return hasRole(Roles.ADMIN) || Objects.equals(listing.getAgentID(), UserContext.getUserId());
    }

    /** Only the agent who posted the listing, or an admin, may change or remove it. */
    private ServiceListing requireOwnedListing(Long id) {
        ServiceListing listing = requireListing(id);
        if (!canEdit(listing)) {
            throw new UserException("You are not allowed to modify service listing with id: " + id);
        }
        return listing;
    }

    private static boolean hasRole(Roles role) {
        var roles = UserContext.getRoles();
        return roles != null && roles.contains(role);
    }

    private static void addEqual(List<Predicate> predicates, CriteriaBuilder cb,
                                 Path<Object> path, Object value) {
        if (value != null) {
            predicates.add(cb.equal(path, value));
        }
    }

    private static void addIn(List<Predicate> predicates, Path<Object> path, List<?> values) {
        if (values != null && !values.isEmpty()) {
            predicates.add(path.in(values));
        }
    }
}
