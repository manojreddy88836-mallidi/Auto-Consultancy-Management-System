package com.autoconsultancy.repository;

import com.autoconsultancy.entity.ApplicationStatusHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationStatusHistoryRepository extends MongoRepository<ApplicationStatusHistory, Long> {
    List<ApplicationStatusHistory> findByApplicationId(Long applicationId);
    void deleteAllByApplicationId(Long applicationId);
}
