package com.autoconsultancy.repository;

import com.autoconsultancy.entity.FinanceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FinanceDetailRepository extends JpaRepository<FinanceDetail, Long>, JpaSpecificationExecutor<FinanceDetail> {
    java.util.List<FinanceDetail> findAllByUnderFinanceTrue();
}
