package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeVariant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BikeVariantRepository extends MongoRepository<BikeVariant, Long> {
    List<BikeVariant> findByBikeModelIdAndActiveTrue(Long modelId);
}
