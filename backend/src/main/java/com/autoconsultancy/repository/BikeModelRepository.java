package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeInventory;
import com.autoconsultancy.entity.BikeModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface BikeModelRepository extends JpaRepository<BikeModel, Long>, JpaSpecificationExecutor<BikeModel> {
    List<BikeModel> findByManufacturerIdAndActiveTrue(Long manufacturerId);
    List<BikeModel> findBySaleStatusAndActiveTrueAndAvailableForSaleTrue(BikeModel.SaleStatus saleStatus);

    /**
     * Returns a map of bikeModelId -> count of AVAILABLE active inventory bikes.
     * Single efficient query; avoids N+1 when rendering the models list.
     */
    @Query("SELECT b.bikeModel.id AS modelId, COUNT(b) AS cnt " +
           "FROM BikeInventory b " +
           "WHERE b.active = true " +
           "  AND b.saleStatus = com.autoconsultancy.entity.BikeInventory$SaleStatus.AVAILABLE " +
           "GROUP BY b.bikeModel.id")
    List<Object[]> countAvailableInventoryPerModel();

    /**
     * Count available inventory for a specific model (used after individual model loads).
     */
    @Query("SELECT COUNT(b) FROM BikeInventory b " +
           "WHERE b.bikeModel.id = :modelId " +
           "  AND b.active = true " +
           "  AND b.saleStatus = com.autoconsultancy.entity.BikeInventory$SaleStatus.AVAILABLE")
    long countAvailableInventoryForModel(@Param("modelId") Long modelId);
}
