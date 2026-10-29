package net.jemsit.product.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.api.MediaApi;
import net.jemsit.common.api.OperatorFeedApi;
import net.jemsit.common.data.enums.EventMessages;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.data.enums.property.OccupancyStatus;
import net.jemsit.common.dto.message.MediaUploaded;
import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.request.product.secondhand.SecondHandFilterRequestDTO;
import net.jemsit.common.dto.request.product.secondhand.SecondHandRequestDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.dto.response.product.secondhand.SecondHandResponseDTO;
import net.jemsit.common.dto.response.product.secondhand.SecondHandStats;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.product.data.model.secondhand.SecondHandItem;
import net.jemsit.product.data.model.secondhand.SecondHandMediaData;
import net.jemsit.product.data.repository.secondhand.SecondHandItemRepository;
import net.jemsit.product.data.repository.secondhand.SecondHandMediaDataRepository;
import net.jemsit.product.mapper.LocationNames;
import net.jemsit.product.mapper.SecondHandMapper;
import net.jemsit.product.service.SecondHandService;
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
public class SecondHandServiceImpl implements SecondHandService {

    private final SecondHandItemRepository itemRepository;
    private final SecondHandMediaDataRepository mediaDataRepository;
    private final SecondHandMapper mapper;
    // media calls back into product via ProductApi, so this edge stays lazy to avoid a startup cycle
    @Lazy
    private final MediaApi mediaApi;
    private final AuthApi authApi;
    private final OperatorFeedApi operatorFeedApi;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SecondHandResponseDTO createDraft(Long userId) {
        SecondHandItem item = new SecondHandItem();
        item.setListingStatus(ListingStatus.DRAFT);
        item.setOccupancyStatus(OccupancyStatus.AVAILABLE);
        UserDetailsResponseDTO userDetails = authApi.getById(userId);
        item.setAgentID(userDetails.id());
        item.setAgent(userDetails.username());
        return toDto(itemRepository.save(item));
    }

    @Override
    @Transactional
    public SecondHandResponseDTO update(Long id, SecondHandRequestDTO request) {
        SecondHandItem item = requireOwnedItem(id);
        mapper.update(request, item);
        return toDto(itemRepository.save(item));
    }

    @Override
    public SecondHandResponseDTO getById(Long id) {
        return toDto(requireVisibleItem(id));
    }

    @Override
    @Transactional
    public String deleteById(Long id) {
        SecondHandItem item = requireOwnedItem(id);
        for (SecondHandMediaData media : item.getMedias()) {
            mediaApi.deleteMedia(media.getMediaURL());
        }
        itemRepository.delete(item);
        return "Second-hand item with id " + id + " has been deleted successfully.";
    }

    @Override
    public void requireEditable(Long id) {
        requireOwnedItem(id);
    }

    // Called back from the async media upload, where UserContext is empty; ownership was checked on
    // the request thread through requireEditable before the upload was queued.
    @Override
    @Transactional
    public SecondHandResponseDTO addImages(AddPropertyImagesRequestDTO request, Long userId) {
        SecondHandItem item = requireItem(request.id());
        boolean hasCoverImage = item.getMedias().stream().anyMatch(SecondHandMediaData::getIsCoverImage);
        for (String url : request.urls()) {
            SecondHandMediaData image = new SecondHandMediaData();
            image.setMediaURL(url);
            image.setIsCoverImage(!hasCoverImage);
            hasCoverImage = true;
            item.addMedia(image);
        }
        SecondHandItem updated = itemRepository.save(item);
        eventPublisher.publishEvent(new MediaUploaded(String.valueOf(userId), EventMessages.MEDIA_UPDATE));
        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteImage(Long id, Long userId) {
        SecondHandMediaData media = mediaDataRepository.findById(id)
                .orElseThrow(() -> new UserException("Second-hand media not found with id: " + id));
        SecondHandItem item = requireOwnedItem(media.getItem().getId());

        mediaApi.deleteMedia(media.getMediaURL());
        item.getMedias().remove(media);
        if (Boolean.TRUE.equals(media.getIsCoverImage()) && !item.getMedias().isEmpty()) {
            item.getMedias().getFirst().setIsCoverImage(true);
        }
        itemRepository.save(item);
        eventPublisher.publishEvent(new MediaUploaded(String.valueOf(userId), EventMessages.MEDIA_UPDATE));
    }

    @Override
    public Integer getMediaCount(Long itemId) {
        return requireVisibleItem(itemId).getMedias().size();
    }

    @Override
    public Page<SecondHandResponseDTO> getAgentsAll(Pageable pageable) {
        Page<SecondHandItem> items = hasRole(Roles.ADMIN)
                ? itemRepository.findAll(pageable)
                : itemRepository.findByAgentID(UserContext.getUserId(), pageable);
        return items.map(this::toDto);
    }

    @Override
    public Page<SecondHandResponseDTO> filter(SecondHandFilterRequestDTO filter, Pageable pageable) {
        // With `mine` (the admin panel) agents see only their own items in any status and admins see
        // everything. Without it the caller is treated as the public, whatever token they carry, so
        // an agent browsing the client sees the same live listings as a visitor - sold items included
        // in what is hidden from them.
        boolean mine = Boolean.TRUE.equals(filter.mine());
        boolean isAdmin = hasRole(Roles.ADMIN);
        boolean isAgent = !isAdmin && hasRole(Roles.AGENT);
        boolean staff = mine && (isAdmin || isAgent);
        boolean isPublic = !staff;
        ListingStatus status = staff ? filter.listingStatus() : ListingStatus.ACTIVE;
        Long agentScope = mine && isAgent ? UserContext.getUserId() : null;

        Specification<SecondHandItem> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (agentScope != null) {
                predicates.add(cb.equal(root.get("agentID"), agentScope));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("listingStatus"), status));
            }
            if (isPublic) {
                // Sold items leave the public browse list; the owner and admins still see them.
                // Null counts as available: rows written before this field existed have no value.
                predicates.add(cb.or(
                        cb.isNull(root.get("occupancyStatus")),
                        cb.notEqual(root.get("occupancyStatus"), OccupancyStatus.SOLD)));
            }
            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
            }
            addEqual(predicates, cb, root.get("offerType"), filter.offerType());
            addEqual(predicates, cb, root.get("regionId"), filter.regionId());
            addEqual(predicates, cb, root.get("districtId"), filter.districtId());
            addEqual(predicates, cb, root.get("placeId"), filter.placeId());
            addIn(predicates, root.get("subCategory"), filter.subCategories());
            addIn(predicates, root.get("condition"), filter.conditions());
            if (filter.priceFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.priceFrom()));
            }
            if (filter.priceTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.priceTo()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return itemRepository.findAll(specification, pageable).map(this::toDto);
    }

    @Override
    public SecondHandStats getStats() {
        return itemRepository.getSecondHandStats(hasRole(Roles.ADMIN) ? null : UserContext.getUserId());
    }

    @Override
    @Transactional
    public long incrementViews(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new UserException("Second-hand item not found with id: " + id);
        }
        itemRepository.incrementViewCount(id);
        long views = itemRepository.findViewCountById(id);
        // Predicate first: most views are not milestones, and this is a public, unauthenticated route
        // that should not pay for a title lookup it will throw away.
        if (operatorFeedApi.isViewMilestone(views)) {
            operatorFeedApi.viewMilestone("second-hand", id, views, itemRepository.findTitleById(id));
        }
        return views;
    }

    private SecondHandResponseDTO toDto(SecondHandItem item) {
        // Refs cleared by a dataset migration resolve to null; hand clients blanks, not "null".
        List<String> shortAddress = item.getId() == null ? List.of("", "", "", "")
                : itemRepository.findShortAddressById(item.getId(), LocationNames.currentLang()).stream()
                .map(part -> part == null ? "" : part)
                .toList();
        return mapper.toDto(item, shortAddress);
    }

    private SecondHandItem requireItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new UserException("Second-hand item not found with id: " + id));
    }

    /** Live items are public; drafts and archived ones only to their agent or an admin. */
    private SecondHandItem requireVisibleItem(Long id) {
        SecondHandItem item = requireItem(id);
        if (item.getListingStatus() != ListingStatus.ACTIVE && !canEdit(item)) {
            throw new UserException("Second-hand item not found with id: " + id);
        }
        return item;
    }

    private static boolean canEdit(SecondHandItem item) {
        return hasRole(Roles.ADMIN) || Objects.equals(item.getAgentID(), UserContext.getUserId());
    }

    /** Only the agent who listed the item, or an admin, may change or remove it. */
    private SecondHandItem requireOwnedItem(Long id) {
        SecondHandItem item = requireItem(id);
        if (!canEdit(item)) {
            throw new UserException("You are not allowed to modify second-hand item with id: " + id);
        }
        return item;
    }

    private static boolean hasRole(Roles role) {
        var roles = UserContext.getRoles();
        return roles != null && roles.contains(role);
    }

    private static void addEqual(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
                                 jakarta.persistence.criteria.Path<Object> path, Object value) {
        if (value != null) {
            predicates.add(cb.equal(path, value));
        }
    }

    private static void addIn(List<Predicate> predicates, jakarta.persistence.criteria.Path<Object> path,
                              List<?> values) {
        if (values != null && !values.isEmpty()) {
            predicates.add(path.in(values));
        }
    }
}
