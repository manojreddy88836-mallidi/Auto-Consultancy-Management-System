package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BikeImageRepository extends JpaRepository<BikeImage, Long> {
    List<BikeImage> findByBikeModelIdOrderByPrimaryDescCreatedAtAsc(Long bikeModelId);
    Optional<BikeImage> findByBikeModelIdAndPrimaryTrue(Long bikeModelId);
    long countByBikeModelId(Long bikeModelId);
}
