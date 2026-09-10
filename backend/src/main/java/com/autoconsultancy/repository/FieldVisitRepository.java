package com.autoconsultancy.repository;
import com.autoconsultancy.entity.FieldVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FieldVisitRepository extends JpaRepository<FieldVisit, Long> {
    Optional<FieldVisit> findByWorkerTaskId(Long taskId);
}
