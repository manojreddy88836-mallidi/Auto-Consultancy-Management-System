package com.autoconsultancy.repository;

import com.autoconsultancy.entity.EmiOverdueAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmiOverdueAlertRepository extends JpaRepository<EmiOverdueAlert, Long> {

    boolean existsByFinanceDetailIdAndOverdueStatusAndAlertMonth(
        Long financeDetailId, String overdueStatus, String alertMonth);
}
