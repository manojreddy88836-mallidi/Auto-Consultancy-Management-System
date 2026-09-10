package com.autoconsultancy.repository;
import com.autoconsultancy.entity.ServiceJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ServiceJobRepository extends JpaRepository<ServiceJob, Long> {
    Optional<ServiceJob> findByWorkerTaskId(Long taskId);
}
