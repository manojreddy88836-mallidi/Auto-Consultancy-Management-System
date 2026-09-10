package com.autoconsultancy.repository;
import com.autoconsultancy.entity.BikeRecovery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface BikeRecoveryRepository extends JpaRepository<BikeRecovery, Long> {
    Optional<BikeRecovery> findByWorkerTaskId(Long taskId);
}
