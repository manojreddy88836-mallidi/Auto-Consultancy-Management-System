package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BikeDetailRepository extends MongoRepository<BikeDetail, Long> {
}
