package net.jemsit.product.data.model.car;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.jemsit.product.data.model.BaseEntity;

/**
 * A car manufacturer from the reference catalog (Chevrolet, Toyota, ...). Pure lookup data for
 * the add-car form's brand dropdown; listings keep their own {@code brand}/{@code model} strings,
 * so nothing breaks when a brand is renamed or removed here.
 */
@Entity
@Table(name = "car_brands")
@Getter
@Setter
public class CarBrand extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
