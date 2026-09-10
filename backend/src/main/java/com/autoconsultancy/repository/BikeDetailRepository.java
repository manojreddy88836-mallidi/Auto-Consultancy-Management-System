package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BikeDetailRepository extends JpaRepository<BikeDetail, Long>, JpaSpecificationExecutor<BikeDetail> {
}
