package com.autoconsultancy.repository;

import com.autoconsultancy.entity.ManufacturingYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManufacturingYearRepository extends JpaRepository<ManufacturingYear, Long>, JpaSpecificationExecutor<ManufacturingYear> {
    List<ManufacturingYear> findByBikeModelIdAndActiveTrue(Long modelId);
}
