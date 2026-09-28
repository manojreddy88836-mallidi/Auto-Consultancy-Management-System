package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeInventoryImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BikeInventoryImageRepository extends MongoRepository<BikeInventoryImage, Long> {

    List<BikeInventoryImage> findByBikeInventoryIdOrderByPrimaryDescCreatedAtAsc(Long bikeInventoryId);

    Optional<BikeInventoryImage> findByBikeInventoryIdAndPrimaryTrue(Long bikeInventoryId);

    long countByBikeInventoryId(Long bikeInventoryId);

    void deleteByBikeInventoryId(Long bikeInventoryId);
}
