package com.autoconsultancy.repository;

import com.autoconsultancy.entity.FinanceDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinanceDetailRepository extends MongoRepository<FinanceDetail, Long> {
    List<FinanceDetail> findAllByUnderFinanceTrue();
}
