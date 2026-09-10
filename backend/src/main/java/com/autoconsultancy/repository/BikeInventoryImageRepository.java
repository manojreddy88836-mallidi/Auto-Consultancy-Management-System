package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeInventoryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BikeInventoryImageRepository extends JpaRepository<BikeInventoryImage, Long> {

    List<BikeInventoryImage> findByBikeInventoryIdOrderByPrimaryDescCreatedAtAsc(Long bikeInventoryId);

    Optional<BikeInventoryImage> findByBikeInventoryIdAndPrimaryTrue(Long bikeInventoryId);

    long countByBikeInventoryId(Long bikeInventoryId);

    void deleteByBikeInventoryId(Long bikeInventoryId);
}
