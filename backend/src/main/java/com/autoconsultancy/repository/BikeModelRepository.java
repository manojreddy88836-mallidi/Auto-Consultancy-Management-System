package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface BikeModelRepository extends MongoRepository<BikeModel, Long> {
    List<BikeModel> findByManufacturerIdAndActiveTrue(Long manufacturerId);
    Optional<BikeModel> findByManufacturerIdAndModelName(Long manufacturerId, String modelName);
    List<BikeModel> findBySaleStatusAndActiveTrueAndAvailableForSaleTrue(BikeModel.SaleStatus saleStatus);
    List<BikeModel> findByActiveTrue();

    default List<Object[]> countAvailableInventoryPerModel() {
        return new ArrayList<>();
    }

    default long countAvailableInventoryForModel(Long modelId) {
        return 0L;
    }
}
