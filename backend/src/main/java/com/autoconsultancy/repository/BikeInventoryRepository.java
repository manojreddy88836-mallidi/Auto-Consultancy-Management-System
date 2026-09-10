package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BikeInventoryRepository extends JpaRepository<BikeInventory, Long> {

    /** Available bikes that have at least one image — for customer listing */
    @Query("SELECT b FROM BikeInventory b WHERE b.saleStatus = com.autoconsultancy.entity.BikeInventory$SaleStatus.AVAILABLE AND b.active = true " +
           "AND EXISTS (SELECT i FROM BikeInventoryImage i WHERE i.bikeInventory = b)")
    List<BikeInventory> findAvailableWithImages();

    /** Available bikes for a specific manufacturer, with image */
    @Query("SELECT b FROM BikeInventory b WHERE b.saleStatus = com.autoconsultancy.entity.BikeInventory$SaleStatus.AVAILABLE AND b.active = true " +
           "AND b.bikeModel.manufacturer.id = :manufacturerId " +
           "AND EXISTS (SELECT i FROM BikeInventoryImage i WHERE i.bikeInventory = b)")
    List<BikeInventory> findAvailableWithImagesByManufacturer(@Param("manufacturerId") Long manufacturerId);

    /** Admin paginated list with optional filters */
    @Query("SELECT b FROM BikeInventory b WHERE b.active = true " +
           "AND (:modelId IS NULL OR b.bikeModel.id = :modelId) " +
           "AND (:manufacturerId IS NULL OR b.bikeModel.manufacturer.id = :manufacturerId) " +
           "AND (:saleStatus IS NULL OR CAST(b.saleStatus AS string) = :saleStatus) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(b.bikeModel.modelName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "     OR LOWER(b.bikeCode) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "     OR LOWER(b.registrationNumber) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<BikeInventory> findAllAdmin(@Param("modelId") Long modelId,
                                     @Param("manufacturerId") Long manufacturerId,
                                     @Param("saleStatus") String saleStatus,
                                     @Param("search") String search,
                                     Pageable pageable);

    /** Count by status (for dashboard) */
    long countBySaleStatus(BikeInventory.SaleStatus saleStatus);

    /** Count active bikes without images */
    @Query("SELECT COUNT(b) FROM BikeInventory b WHERE b.active = true " +
           "AND NOT EXISTS (SELECT i FROM BikeInventoryImage i WHERE i.bikeInventory = b)")
    long countWithoutImages();
}
