package com.autoconsultancy.repository;

import com.autoconsultancy.entity.WorkerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerAssignmentRepository extends JpaRepository<WorkerAssignment, Long>, JpaSpecificationExecutor<WorkerAssignment> {
    Optional<WorkerAssignment> findByApplicationId(Long applicationId);
}
