package com.autoconsultancy.repository;

import com.autoconsultancy.entity.EmiOverdueAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmiOverdueAlertRepository extends MongoRepository<EmiOverdueAlert, Long> {

    boolean existsByFinanceDetailIdAndOverdueStatusAndAlertMonth(
        Long financeDetailId, String overdueStatus, String alertMonth);
}
