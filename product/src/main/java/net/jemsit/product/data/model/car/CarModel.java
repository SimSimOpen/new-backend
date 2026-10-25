package net.jemsit.product.data.model.car;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.jemsit.product.data.model.BaseEntity;

/**
 * A model belonging to a {@link CarBrand} catalog entry (Cobalt under Chevrolet, ...). The
 * model list shown in the add-car form is filtered by the chosen brand.
 */
@Entity
@Table(name = "car_models", uniqueConstraints = @UniqueConstraint(columnNames = {"brand_id", "name"}))
@Getter
@Setter
public class CarModel extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private CarBrand brand;
}
