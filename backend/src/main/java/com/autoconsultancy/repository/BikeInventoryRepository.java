package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public interface BikeInventoryRepository extends MongoRepository<BikeInventory, Long> {

    @Query("{'saleStatus': 'AVAILABLE', 'active': true}")
    List<BikeInventory> findAvailableWithImages();

    @Query("{'saleStatus': 'AVAILABLE', 'active': true, 'bikeModel.manufacturer.id': ?0}")
    List<BikeInventory> findAvailableWithImagesByManufacturer(Long manufacturerId);

    long countBySaleStatus(BikeInventory.SaleStatus saleStatus);

    List<BikeInventory> findByActiveTrue();

    default Page<BikeInventory> findAllAdmin(Long modelId, Long manufacturerId, String saleStatus, String search, Pageable pageable) {
        List<BikeInventory> list = findByActiveTrue();
        if (modelId != null) {
            list = list.stream().filter(b -> b.getBikeModel() != null && modelId.equals(b.getBikeModel().getId())).collect(Collectors.toList());
        }
        if (manufacturerId != null) {
            list = list.stream().filter(b -> b.getBikeModel() != null && b.getBikeModel().getManufacturer() != null && manufacturerId.equals(b.getBikeModel().getManufacturer().getId())).collect(Collectors.toList());
        }
        if (saleStatus != null && !saleStatus.isBlank()) {
            list = list.stream().filter(b -> b.getSaleStatus() != null && b.getSaleStatus().name().equalsIgnoreCase(saleStatus)).collect(Collectors.toList());
        }
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            list = list.stream().filter(b ->
                    (b.getBikeModel() != null && b.getBikeModel().getModelName() != null && b.getBikeModel().getModelName().toLowerCase().contains(q)) ||
                    (b.getBikeCode() != null && b.getBikeCode().toLowerCase().contains(q)) ||
                    (b.getRegistrationNumber() != null && b.getRegistrationNumber().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<BikeInventory> content = (start <= list.size()) ? list.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(content, pageable, list.size());
    }
}
