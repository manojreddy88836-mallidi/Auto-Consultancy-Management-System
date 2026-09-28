package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeRecovery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BikeRecoveryRepository extends MongoRepository<BikeRecovery, Long> {
    Optional<BikeRecovery> findByWorkerTaskId(Long taskId);
}
