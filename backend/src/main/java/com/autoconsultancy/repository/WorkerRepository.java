package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long>, JpaSpecificationExecutor<Worker> {
}
