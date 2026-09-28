package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BikeImageRepository extends MongoRepository<BikeImage, Long> {
    List<BikeImage> findByBikeModelIdOrderByPrimaryDescCreatedAtAsc(Long bikeModelId);
    Optional<BikeImage> findByBikeModelIdAndPrimaryTrue(Long bikeModelId);
    long countByBikeModelId(Long bikeModelId);
}
