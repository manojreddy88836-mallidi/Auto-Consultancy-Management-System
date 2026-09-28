package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, Long> {

    /** Count customers created since the given datetime — used for "new this month" dashboard stat. */
    @Query(value = "{'createdAt': {$gte: ?0}}", count = true)
    long countByCreatedAtAfter(LocalDateTime since);

    /** Returns count of customers registered in the current calendar month. */
    default long countNewThisMonth() {
        LocalDateTime startOfMonth = java.time.LocalDate.now()
                .withDayOfMonth(1).atStartOfDay();
        return countByCreatedAtAfter(startOfMonth);
    }
}
