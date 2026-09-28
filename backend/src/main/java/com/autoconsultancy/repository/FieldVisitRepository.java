package com.autoconsultancy.repository;

import com.autoconsultancy.entity.FieldVisit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FieldVisitRepository extends MongoRepository<FieldVisit, Long> {
    Optional<FieldVisit> findByWorkerTaskId(Long taskId);
}
