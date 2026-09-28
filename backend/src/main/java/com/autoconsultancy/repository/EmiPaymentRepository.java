package com.autoconsultancy.repository;

import com.autoconsultancy.entity.EmiPayment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmiPaymentRepository extends MongoRepository<EmiPayment, Long> {

    List<EmiPayment> findByFinanceDetailIdOrderByInstallmentNumberAsc(Long financeDetailId);

    Optional<EmiPayment> findByFinanceDetailIdAndInstallmentNumber(Long financeDetailId, Integer installmentNumber);

    long countByFinanceDetailId(Long financeDetailId);

    Optional<EmiPayment> findFirstByFinanceDetailIdOrderByInstallmentNumberDesc(Long financeDetailId);

    boolean existsByFinanceDetailIdAndInstallmentNumber(Long financeDetailId, Integer installmentNumber);
}
