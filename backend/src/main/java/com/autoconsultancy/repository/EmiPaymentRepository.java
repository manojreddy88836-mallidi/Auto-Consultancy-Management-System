package com.autoconsultancy.repository;

import com.autoconsultancy.entity.EmiPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmiPaymentRepository extends JpaRepository<EmiPayment, Long> {

    List<EmiPayment> findByFinanceDetailIdOrderByInstallmentNumberAsc(Long financeDetailId);

    Optional<EmiPayment> findByFinanceDetailIdAndInstallmentNumber(Long financeDetailId, Integer installmentNumber);

    long countByFinanceDetailId(Long financeDetailId);

    @Query("SELECT MAX(ep.installmentNumber) FROM EmiPayment ep WHERE ep.financeDetail.id = :fdId")
    Optional<Integer> findMaxInstallmentNumber(@Param("fdId") Long financeDetailId);

    boolean existsByFinanceDetailIdAndInstallmentNumber(Long financeDetailId, Integer installmentNumber);
}
