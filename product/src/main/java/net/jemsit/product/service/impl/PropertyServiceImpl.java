package net.jemsit.product.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.api.MediaApi;
import net.jemsit.common.data.enums.EventMessages;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.dto.message.MediaUploaded;
import net.jemsit.common.dto.request.product.property.AddPropertyImagesRequestDTO;
import net.jemsit.common.dto.request.product.property.PropertyRequestDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.dto.response.product.propeprty.PropertiesStats;
import net.jemsit.common.dto.response.product.propeprty.PropertyResponseDTO;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.product.data.model.property.Property;
import net.jemsit.product.data.model.property.PropertyAmenities;
import net.jemsit.product.data.model.property.PropertyLocation;
import net.jemsit.product.data.model.property.PropertyMediaData;
import net.jemsit.product.data.repository.property.PropertyMediaDataRepository;
import net.jemsit.product.data.repository.property.PropertyRepository;
import net.jemsit.product.mapper.PropertyMapper;
import net.jemsit.product.service.PropertyService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaDataRepository propertyMediaDataRepository;
    private final PropertyMapper propertyMapper;
    private final PropertyFilterServiceImpl propertyFilterService;
    // media calls back into product via ProductApi, so this edge stays lazy to avoid a startup cycle
    @Lazy
    private final MediaApi mediaApi;
    private final AuthApi authApi;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String add(PropertyRequestDTO request) {
        propertyRepository.save(propertyMapper.toEntity(request));
        return "Property information added to product successfully.";
    }

    @Override
    @Transactional
    public PropertyResponseDTO update(Long id, PropertyRequestDTO request) {
        Property toUpdate = requireProperty(id);

        if (request.title() != null && !request.title().isEmpty()) {
            toUpdate.setTitle(request.title());
        }
        if (request.description() != null) {
            toUpdate.setDescription(request.description());
        }
        if (request.price() != null) {
            toUpdate.setPrice(request.price());
        }
        if (request.numberOfRooms() != null) {
            toUpdate.setNumberOfRooms(request.numberOfRooms());
        }
        if (request.area() != null) {
            toUpdate.setArea(request.area());
        }
        if (request.floor() != null) {
            toUpdate.setFloor(request.floor());
        }
        if (request.totalFloors() != null) {
            toUpdate.setTotalFloors(request.totalFloors());
        }
        if (request.ownerContact() != null && !request.ownerContact().isEmpty()) {
            toUpdate.setOwnerOrAgentContact(request.ownerContact());
        }
        if (request.offerType() != null) {
            toUpdate.setOfferType(request.offerType());
        }
        if (request.type() != null) {
            toUpdate.setType(request.type());
        }
        if (request.category() != null) {
            toUpdate.setCategory(request.category());
        }
        if (request.listingStatus() != null) {
            toUpdate.setListingStatus(request.listingStatus());
        }
        if (request.occupancyStatus() != null) {
            toUpdate.setOccupancyStatus(request.occupancyStatus());
        }
        if (request.publish() != null && !request.publish().isEmpty()) {
            toUpdate.setPublish(request.publish());
        }
        if (request.location() != null) {
            toUpdate.setLocation(applyLocation(request, toUpdate));
        }
        if (request.amenities() != null) {
            toUpdate.setAmenities(applyAmenities(request, toUpdate));
        }

        Property updated = propertyRepository.save(toUpdate);
        return propertyMapper.toDtoWithShortAddress(updated,
                propertyFilterService.getLocationList(updated.getLocation()));
    }

    @Override
    public Page<PropertyResponseDTO> getAll(Pageable pageable) {
        return toDtoPage(propertyRepository.findAll(pageable));
    }

    @Override
    public PropertyResponseDTO getById(Long id) {
        return propertyMapper.toDto(requireProperty(id));
    }

    @Override
    @Transactional
    public String deleteById(Long id) {
        Property property = requireProperty(id);
        for (PropertyMediaData media : property.getMedias()) {
            mediaApi.deleteMedia(media.getMediaURL());
        }
        propertyRepository.delete(property);
        return "Property with id " + id + " has been deleted successfully.";
    }

    @Override
    @Transactional
    public PropertyResponseDTO addPropertyImage(AddPropertyImagesRequestDTO request, Long userId) {
        Property property = requireProperty(request.id());

        boolean hasCoverImage = property.getMedias().stream().anyMatch(PropertyMediaData::getIsCoverImage);
        for (String url : request.urls()) {
            PropertyMediaData image = new PropertyMediaData();
            image.setMediaURL(url);
            image.setIsCoverImage(!hasCoverImage);
            hasCoverImage = true;
            property.addMedia(image);
        }

        Property updated = propertyRepository.save(property);
        eventPublisher.publishEvent(new MediaUploaded(String.valueOf(userId), EventMessages.MEDIA_UPDATE));
        return propertyMapper.toDto(updated);
    }

    @Override
    @Transactional
    public PropertyResponseDTO createPropertyDraft(Long userId) {
        Property property = new Property();
        property.setListingStatus(ListingStatus.DRAFT);
        UserDetailsResponseDTO userDetails = authApi.getById(userId);
        property.setAgentID(userDetails.id());
        property.setAgent(userDetails.username());
        return propertyMapper.toDto(propertyRepository.save(property));
    }

    @Override
    public void deletePropertyImage(Long id, Long userId) {
        PropertyMediaData media = propertyMediaDataRepository.findById(id)
                .orElseThrow(() -> new UserException("Property media not found with id: " + id));

        Long propertyId = media.getProperty().getId();
        boolean wasCoverImage = media.getIsCoverImage();

        mediaApi.deleteMedia(media.getMediaURL());
        propertyMediaDataRepository.delete(media);

        if (wasCoverImage) {
            reAssignCoverImage(propertyId);
        }
        eventPublisher.publishEvent(new MediaUploaded(String.valueOf(userId), EventMessages.MEDIA_UPDATE));
    }

    @Override
    public Page<PropertyResponseDTO> getAgentsAllProperties(Pageable pageable) {
        Page<Property> properties = isAdmin()
                ? propertyRepository.findAll(pageable)
                : propertyRepository.findByAgentID(UserContext.getUserId(), pageable);
        return toDtoPage(properties);
    }

    @Override
    public Page<PropertyResponseDTO> getAllPublished(Pageable pageable) {
        return toDtoPage(propertyRepository.findAllPublished(pageable));
    }

    @Override
    public Integer getPropertyMediaCount(Long propertyId) {
        return requireProperty(propertyId).getMedias().size();
    }

    @Override
    public PropertiesStats getPropertiesStats() {
        return isAdmin()
                ? propertyRepository.getPropertiesStats()
                : propertyRepository.getPropertiesStatsByAgentId(UserContext.getUserId());
    }

    @Override
    @Transactional
    public long incrementViews(Long id) {
        if (!propertyRepository.existsById(id)) {
            throw new UserException("Property not found with id: " + id);
        }
        propertyRepository.incrementViewCount(id);
        return propertyRepository.findViewCountByPropertyId(id);
    }

    private Page<PropertyResponseDTO> toDtoPage(Page<Property> properties) {
        return properties.map(property -> propertyMapper.toDtoWithShortAddress(property,
                propertyFilterService.getLocationList(property.getLocation())));
    }

    private Property requireProperty(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new UserException("Property not found with id: " + id));
    }

    private void reAssignCoverImage(Long propertyId) {
        Property property = requireProperty(propertyId);
        var mediaList = property.getMedias();
        if (mediaList.isEmpty()) {
            return;
        }
        mediaList.getFirst().setIsCoverImage(true);
        propertyRepository.save(property);
    }

    private static boolean isAdmin() {
        var roles = UserContext.getRoles();
        return roles != null && roles.contains(Roles.ADMIN);
    }

    private static PropertyLocation applyLocation(PropertyRequestDTO request, Property toUpdate) {
        var source = request.location();
        var location = toUpdate.getLocation();
        if (location == null) {
            location = new PropertyLocation();
        }
        location.setAddress(source.address());
        location.setCountry(source.country());
        location.setRegionID(source.regionID());
        location.setDistrictID(source.districtID());
        location.setMapLocation(source.mapLocation());
        location.setPlaceID(source.placeID());
        return location;
    }

    private static PropertyAmenities applyAmenities(PropertyRequestDTO request, Property toUpdate) {
        var source = request.amenities();
        var amenities = toUpdate.getAmenities();
        if (amenities == null) {
            amenities = new PropertyAmenities();
        }
        amenities.setHasParking(source.hasParking());
        amenities.setHasElevator(source.hasElevator());
        amenities.setHasGarden(source.hasGarden());
        amenities.setHasSwimmingPool(source.hasSwimmingPool());
        amenities.setHasSecurity(source.hasSecurity());
        amenities.setHasGym(source.hasGym());
        amenities.setHasWashingMachine(source.hasWashingMachine());
        amenities.setHasAirConditioning(source.hasAirConditioning());
        amenities.setHasInternet(source.hasInternet());
        amenities.setHasRefrigerator(source.hasRefrigerator());
        amenities.setHasDishwasher(source.hasDishwasher());
        amenities.setHasMicrowave(source.hasMicrowave());
        amenities.setHasParkingSpace(source.hasParkingSpace());
        amenities.setHasTV(source.hasTV());
        amenities.setHasSatellite(source.hasSatellite());
        amenities.setHasFurniture(source.hasFurniture());
        return amenities;
    }
}
