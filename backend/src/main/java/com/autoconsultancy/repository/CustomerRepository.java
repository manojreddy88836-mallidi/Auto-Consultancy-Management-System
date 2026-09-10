package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    @Query("SELECT COUNT(c) FROM Customer c WHERE MONTH(c.createdAt) = MONTH(CURRENT_DATE) AND YEAR(c.createdAt) = YEAR(CURRENT_DATE)")
    long countNewThisMonth();
}
