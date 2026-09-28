package com.autoconsultancy.repository;

import com.autoconsultancy.entity.WorkerAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerAssignmentRepository extends MongoRepository<WorkerAssignment, Long> {
    Optional<WorkerAssignment> findByApplicationId(Long applicationId);
}
