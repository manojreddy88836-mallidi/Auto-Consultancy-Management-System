package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Worker;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerRepository extends MongoRepository<Worker, Long> {
    Optional<Worker> findByUserId(Long userId);
}
