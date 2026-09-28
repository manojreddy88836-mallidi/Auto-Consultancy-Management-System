package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, Long> {

    default long countNewThisMonth() {
        return count();
    }
}
