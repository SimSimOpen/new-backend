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
import net.jemsit.common.dto.message.MediaUploaded;
import net.jemsit.common.dto.request.product.car.CarFilterRequestDTO;
import net.jemsit.common.dto.request.product.car.CarRequestDTO;
import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.dto.response.product.car.CarBrandDto;
import net.jemsit.common.dto.response.product.car.CarResponseDTO;
import net.jemsit.common.dto.response.product.car.CarsStats;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.product.data.model.car.Car;
import net.jemsit.product.data.model.car.CarBrand;
import net.jemsit.product.data.model.car.CarMediaData;
import net.jemsit.product.data.model.car.CarModel;
import net.jemsit.product.data.repository.car.CarBrandRepository;
import net.jemsit.product.data.repository.car.CarMediaDataRepository;
import net.jemsit.product.data.repository.car.CarModelRepository;
import net.jemsit.product.data.repository.car.CarRepository;
import net.jemsit.product.mapper.CarMapper;
import net.jemsit.product.mapper.LocationNames;
import net.jemsit.product.service.CarService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final CarMediaDataRepository carMediaDataRepository;
    private final CarBrandRepository carBrandRepository;
    private final CarModelRepository carModelRepository;
    private final CarMapper carMapper;
    // media calls back into product via ProductApi, so this edge stays lazy to avoid a startup cycle
    @Lazy
    private final MediaApi mediaApi;
    private final AuthApi authApi;
    private final OperatorFeedApi operatorFeedApi;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CarResponseDTO createDraft(Long userId) {
        Car car = new Car();
        car.setListingStatus(ListingStatus.DRAFT);
        UserDetailsResponseDTO userDetails = authApi.getById(userId);
        car.setAgentID(userDetails.id());
        car.setAgent(userDetails.username());
        return toDto(carRepository.save(car));
    }

    @Override
    @Transactional
    public CarResponseDTO update(Long id, CarRequestDTO request) {
        Car car = requireOwnedCar(id);
        carMapper.update(request, car);
        return toDto(carRepository.save(car));
    }

    @Override
    public CarResponseDTO getById(Long id) {
        return toDto(requireVisibleCar(id));
    }

    @Override
    @Transactional
    public String deleteById(Long id) {
        Car car = requireOwnedCar(id);
        for (CarMediaData media : car.getMedias()) {
            mediaApi.deleteMedia(media.getMediaURL());
        }
        carRepository.delete(car);
        return "Car with id " + id + " has been deleted successfully.";
    }

    @Override
    public void requireEditable(Long id) {
        requireOwnedCar(id);
    }

    // Called back from the async media upload, where UserContext is empty; ownership was checked on
    // the request thread through requireEditable before the upload was queued.
    @Override
    @Transactional
    public CarResponseDTO addImages(AddPropertyImagesRequestDTO request, Long userId) {
        Car car = requireCar(request.id());
        boolean hasCoverImage = car.getMedias().stream().anyMatch(CarMediaData::getIsCoverImage);
        for (String url : request.urls()) {
            CarMediaData image = new CarMediaData();
            image.setMediaURL(url);
            image.setIsCoverImage(!hasCoverImage);
            hasCoverImage = true;
            car.addMedia(image);
        }
        Car updated = carRepository.save(car);
        eventPublisher.publishEvent(new MediaUploaded(String.valueOf(userId), EventMessages.MEDIA_UPDATE));
        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteImage(Long id, Long userId) {
        CarMediaData media = carMediaDataRepository.findById(id)
                .orElseThrow(() -> new UserException("Car media not found with id: " + id));
        Car car = requireOwnedCar(media.getCar().getId());

        mediaApi.deleteMedia(media.getMediaURL());
        car.getMedias().remove(media);
        if (Boolean.TRUE.equals(media.getIsCoverImage()) && !car.getMedias().isEmpty()) {
            car.getMedias().getFirst().setIsCoverImage(true);
        }
        carRepository.save(car);
        eventPublisher.publishEvent(new MediaUploaded(String.valueOf(userId), EventMessages.MEDIA_UPDATE));
    }

    @Override
    public Integer getMediaCount(Long carId) {
        return requireVisibleCar(carId).getMedias().size();
    }

    @Override
    public Page<CarResponseDTO> getAgentsAll(Pageable pageable) {
        Page<Car> cars = hasRole(Roles.ADMIN)
                ? carRepository.findAll(pageable)
                : carRepository.findByAgentID(UserContext.getUserId(), pageable);
        return cars.map(this::toDto);
    }

    @Override
    public Page<CarResponseDTO> filter(CarFilterRequestDTO filter, Pageable pageable) {
        // With `mine` (the admin panel) agents see only their own cars in any status and admins see
        // everything. Without it the caller is treated as the public, whatever token they carry, so
        // an agent browsing the client sees the same live listings as a visitor.
        boolean mine = Boolean.TRUE.equals(filter.mine());
        boolean isAdmin = hasRole(Roles.ADMIN);
        boolean isAgent = !isAdmin && hasRole(Roles.AGENT);
        ListingStatus status = mine && (isAdmin || isAgent) ? filter.listingStatus() : ListingStatus.ACTIVE;
        Long agentScope = mine && isAgent ? UserContext.getUserId() : null;

        Specification<Car> specification = (root, query, cb) -> {
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
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("brand")), pattern),
                        cb.like(cb.lower(root.get("model")), pattern)));
            }
            if (filter.brand() != null && !filter.brand().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), filter.brand().trim().toLowerCase()));
            }
            if (filter.model() != null && !filter.model().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("model")), filter.model().trim().toLowerCase()));
            }
            addEqual(predicates, cb, root.get("offerType"), filter.offerType());
            addEqual(predicates, cb, root.get("occupancyStatus"), filter.occupancyStatus());
            addEqual(predicates, cb, root.get("condition"), filter.condition());
            addEqual(predicates, cb, root.get("regionId"), filter.regionId());
            addEqual(predicates, cb, root.get("districtId"), filter.districtId());
            addEqual(predicates, cb, root.get("placeId"), filter.placeId());
            addIn(predicates, root.get("bodyType"), filter.bodyTypes());
            addIn(predicates, root.get("fuelType"), filter.fuelTypes());
            addIn(predicates, root.get("transmission"), filter.transmissions());
            addIn(predicates, root.get("driveType"), filter.driveTypes());
            if (filter.priceFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.priceFrom()));
            }
            if (filter.priceTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.priceTo()));
            }
            if (filter.yearFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("year"), filter.yearFrom()));
            }
            if (filter.yearTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("year"), filter.yearTo()));
            }
            if (filter.mileageTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("mileage"), filter.mileageTo()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return carRepository.findAll(specification, pageable).map(this::toDto);
    }

    @Override
    public CarsStats getStats() {
        return carRepository.getCarsStats(hasRole(Roles.ADMIN) ? null : UserContext.getUserId());
    }

    /**
     * The full brand/model catalog for the add-car dropdowns. Two queries: brands, then every
     * model grouped by its brand in memory, so a brand never triggers its own round trip.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CarBrandDto> getBrands() {
        Map<Long, List<String>> modelsByBrand = carModelRepository.findAllWithBrands().stream()
                .collect(Collectors.groupingBy(
                        model -> model.getBrand().getId(),
                        Collectors.mapping(CarModel::getName, Collectors.toList())));
        return carBrandRepository.findAllByIsDeletedFalseOrderByNameAsc().stream()
                .map(brand -> new CarBrandDto(brand.getName(),
                        modelsByBrand.getOrDefault(brand.getId(), List.of()).stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList()))
                .toList();
    }

    @Override
    @Transactional
    public long incrementViews(Long id) {
        if (!carRepository.existsById(id)) {
            throw new UserException("Car not found with id: " + id);
        }
        carRepository.incrementViewCount(id);
        long views = carRepository.findViewCountById(id);
        // Predicate first: most views are not milestones, and this is a public, unauthenticated route
        // that should not pay for a name lookup it will throw away.
        if (operatorFeedApi.isViewMilestone(views)) {
            operatorFeedApi.viewMilestone("car", id, views, displayName(id));
        }
        return views;
    }

    /** Brand and model when present, else the free-text title; null when the row carries neither. */
    private String displayName(Long id) {
        List<Object[]> rows = carRepository.findNamePartsById(id);
        if (rows.isEmpty()) {
            return null;
        }
        Object[] parts = rows.get(0);
        StringBuilder name = new StringBuilder();
        for (Object part : new Object[] { parts[0], parts[1] }) {
            if (part != null && !part.toString().isBlank()) {
                if (!name.isEmpty()) {
                    name.append(' ');
                }
                name.append(part.toString().trim());
            }
        }
        if (!name.isEmpty()) {
            return name.toString();
        }
        return parts[2] != null && !parts[2].toString().isBlank() ? parts[2].toString().trim() : null;
    }

    private CarResponseDTO toDto(Car car) {
        // Refs cleared by a dataset migration resolve to null; hand clients blanks, not "null".
        List<String> shortAddress = car.getId() == null ? List.of("", "", "", "")
                : carRepository.findShortAddressById(car.getId(), LocationNames.currentLang()).stream()
                .map(part -> part == null ? "" : part)
                .toList();
        return carMapper.toDto(car, shortAddress);
    }

    private Car requireCar(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new UserException("Car not found with id: " + id));
    }

    /** Live cars are public; drafts and archived ones only to their agent or an admin. */
    private Car requireVisibleCar(Long id) {
        Car car = requireCar(id);
        if (car.getListingStatus() != ListingStatus.ACTIVE && !canEdit(car)) {
            throw new UserException("Car not found with id: " + id);
        }
        return car;
    }

    private static boolean canEdit(Car car) {
        return hasRole(Roles.ADMIN) || Objects.equals(car.getAgentID(), UserContext.getUserId());
    }

    /** Only the agent who listed the car, or an admin, may change or remove it. */
    private Car requireOwnedCar(Long id) {
        Car car = requireCar(id);
        if (!canEdit(car)) {
            throw new UserException("You are not allowed to modify car with id: " + id);
        }
        return car;
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
