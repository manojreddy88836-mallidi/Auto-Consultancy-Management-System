package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeOffer;
import com.autoconsultancy.entity.BikeOffer.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BikeOfferRepository extends MongoRepository<BikeOffer, Long> {

    List<BikeOffer> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<BikeOffer> findByBikeInventoryIdOrderByCreatedAtDesc(Long bikeInventoryId);

    Page<BikeOffer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BikeOffer> findByStatusOrderByCreatedAtDesc(OfferStatus status, Pageable pageable);

    @Query(value = "{'customer.id': ?0, 'bikeInventory.id': ?1, 'status': {$in: ['PENDING', 'COUNTER_OFFER']}}", exists = true)
    boolean existsActiveOffer(Long customerId, Long bikeInventoryId);

    long countByStatus(OfferStatus status);

    @Query("{'bikeInventory.id': ?0, 'status': {$in: ['PENDING', 'COUNTER_OFFER']}}")
    List<BikeOffer> findActiveOffersByBike(Long bikeInventoryId);

    List<BikeOffer> findByCustomerIdAndBikeInventoryIdOrderByCreatedAtDesc(Long customerId, Long bikeInventoryId);

    default List<BikeOffer> findByCustomerAndBike(Long customerId, Long bikeInventoryId) {
        return findByCustomerIdAndBikeInventoryIdOrderByCreatedAtDesc(customerId, bikeInventoryId);
    }
}
