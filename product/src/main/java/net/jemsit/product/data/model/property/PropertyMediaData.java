package net.jemsit.product.data.model.property;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "property_images")
@Getter
@Setter
public class PropertyMediaData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "media_url", nullable = false)
    private String mediaURL;
    @Column(name = "is_cover_image", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isCoverImage = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
}
