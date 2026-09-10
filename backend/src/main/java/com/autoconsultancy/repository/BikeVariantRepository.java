package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BikeVariantRepository extends JpaRepository<BikeVariant, Long>, JpaSpecificationExecutor<BikeVariant> {
    List<BikeVariant> findByBikeModelIdAndActiveTrue(Long modelId);
}
