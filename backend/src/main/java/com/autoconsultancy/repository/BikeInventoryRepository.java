package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BikeInventoryRepository extends MongoRepository<BikeInventory, Long> {

    @Query("{'saleStatus': 'AVAILABLE', 'active': true}")
    List<BikeInventory> findAvailableWithImages();

    @Query("{'saleStatus': 'AVAILABLE', 'active': true, 'bikeModel.manufacturer.id': ?0}")
    List<BikeInventory> findAvailableWithImagesByManufacturer(Long manufacturerId);

    long countBySaleStatus(BikeInventory.SaleStatus saleStatus);

    /** DB-level paginated: all active — used when no filters are applied. */
    Page<BikeInventory> findByActiveTrue(Pageable pageable);

    /** DB-level paginated: filter by model */
    @Query("{'active': true, 'bikeModel.id': ?0}")
    Page<BikeInventory> findByActiveTrueAndModelId(Long modelId, Pageable pageable);

    /** DB-level paginated: filter by manufacturer */
    @Query("{'active': true, 'bikeModel.manufacturer.id': ?0}")
    Page<BikeInventory> findByActiveTrueAndManufacturerId(Long manufacturerId, Pageable pageable);

    /** DB-level paginated: filter by sale status */
    @Query("{'active': true, 'saleStatus': ?0}")
    Page<BikeInventory> findByActiveTrueAndSaleStatus(String saleStatus, Pageable pageable);

    /** DB-level paginated: filter by model + manufacturer */
    @Query("{'active': true, 'bikeModel.id': ?0, 'bikeModel.manufacturer.id': ?1}")
    Page<BikeInventory> findByActiveTrueAndModelIdAndManufacturerId(Long modelId, Long manufacturerId, Pageable pageable);

    /** DB-level paginated: filter by model + saleStatus */
    @Query("{'active': true, 'bikeModel.id': ?0, 'saleStatus': ?1}")
    Page<BikeInventory> findByActiveTrueAndModelIdAndSaleStatus(Long modelId, String saleStatus, Pageable pageable);

    /** DB-level paginated: filter by manufacturer + saleStatus */
    @Query("{'active': true, 'bikeModel.manufacturer.id': ?0, 'saleStatus': ?1}")
    Page<BikeInventory> findByActiveTrueAndManufacturerIdAndSaleStatus(Long manufacturerId, String saleStatus, Pageable pageable);

    /** DB-level paginated: all three structural filters */
    @Query("{'active': true, 'bikeModel.id': ?0, 'bikeModel.manufacturer.id': ?1, 'saleStatus': ?2}")
    Page<BikeInventory> findByActiveTrueAndModelIdAndManufacturerIdAndSaleStatus(Long modelId, Long manufacturerId, String saleStatus, Pageable pageable);

    // findByActiveTrue() list — kept for internal use by some services
    List<BikeInventory> findByActiveTrue();

    /**
     * NOTE: The findAllAdmin() method with search has been moved to BikeInventoryService.findAllAdmin()
     * which uses MongoTemplate for regex search. This avoids loading all records into memory.
     * Callers should use BikeInventoryService.findAllAdmin() instead.
     */
}
