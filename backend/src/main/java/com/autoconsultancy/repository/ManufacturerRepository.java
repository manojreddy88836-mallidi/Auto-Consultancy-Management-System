package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long>, JpaSpecificationExecutor<Manufacturer> {
    List<Manufacturer> findByActiveTrue();
}
