package net.jemsit.product.data.repository.car;

import net.jemsit.product.data.model.car.CarBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarBrandRepository extends JpaRepository<CarBrand, Long> {
    List<CarBrand> findAllByIsDeletedFalseOrderByNameAsc();

    Optional<CarBrand> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCase(String name);
}
