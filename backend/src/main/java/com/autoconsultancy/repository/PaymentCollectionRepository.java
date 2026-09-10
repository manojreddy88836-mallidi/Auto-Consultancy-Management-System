package com.autoconsultancy.repository;
import com.autoconsultancy.entity.PaymentCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PaymentCollectionRepository extends JpaRepository<PaymentCollection, Long> {
    Optional<PaymentCollection> findByWorkerTaskId(Long taskId);
}
