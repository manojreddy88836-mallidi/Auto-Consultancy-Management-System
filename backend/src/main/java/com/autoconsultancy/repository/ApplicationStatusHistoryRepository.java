package com.autoconsultancy.repository;

import com.autoconsultancy.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long>, JpaSpecificationExecutor<ApplicationStatusHistory> {
    List<ApplicationStatusHistory> findByApplicationId(Long applicationId);
    void deleteAllByApplicationId(Long applicationId);
}
