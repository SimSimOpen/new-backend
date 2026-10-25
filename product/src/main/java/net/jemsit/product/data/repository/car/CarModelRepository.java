package net.jemsit.product.data.repository.car;

import net.jemsit.product.data.model.car.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    List<CarModel> findAllByBrandIdAndIsDeletedFalseOrderByNameAsc(Long brandId);

    /** Whole catalog in one query with brands fetched eagerly; the service groups rows by brand. */
    @Query("SELECT m FROM CarModel m JOIN FETCH m.brand WHERE m.isDeleted = false")
    List<CarModel> findAllWithBrands();
}
