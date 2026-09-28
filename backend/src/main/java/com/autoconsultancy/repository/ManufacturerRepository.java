package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Manufacturer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManufacturerRepository extends MongoRepository<Manufacturer, Long> {
    List<Manufacturer> findByActiveTrue();
}
