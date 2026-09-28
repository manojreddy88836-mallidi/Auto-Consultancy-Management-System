package com.autoconsultancy.repository;

import com.autoconsultancy.entity.ServiceJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceJobRepository extends MongoRepository<ServiceJob, Long> {
    Optional<ServiceJob> findByWorkerTaskId(Long taskId);
}
