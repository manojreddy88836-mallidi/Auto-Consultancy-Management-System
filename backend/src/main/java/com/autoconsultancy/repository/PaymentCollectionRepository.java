package com.autoconsultancy.repository;

import com.autoconsultancy.entity.PaymentCollection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentCollectionRepository extends MongoRepository<PaymentCollection, Long> {
    Optional<PaymentCollection> findByWorkerTaskId(Long taskId);
}
