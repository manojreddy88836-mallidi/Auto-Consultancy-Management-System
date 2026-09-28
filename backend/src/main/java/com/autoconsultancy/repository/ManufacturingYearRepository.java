package com.autoconsultancy.repository;

import com.autoconsultancy.entity.ManufacturingYear;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManufacturingYearRepository extends MongoRepository<ManufacturingYear, Long> {
    List<ManufacturingYear> findByBikeModelIdAndActiveTrue(Long modelId);
}
