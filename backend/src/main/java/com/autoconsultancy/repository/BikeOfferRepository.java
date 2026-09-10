package com.autoconsultancy.repository;

import com.autoconsultancy.entity.BikeOffer;
import com.autoconsultancy.entity.BikeOffer.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BikeOfferRepository extends JpaRepository<BikeOffer, Long> {

    /** All offers made by a specific customer, newest first */
    List<BikeOffer> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /** All offers on a specific bike */
    List<BikeOffer> findByBikeInventoryIdOrderByCreatedAtDesc(Long bikeInventoryId);

    /** Paginated list for admin/worker */
    Page<BikeOffer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Paginated list filtered by status */
    Page<BikeOffer> findByStatusOrderByCreatedAtDesc(OfferStatus status, Pageable pageable);

    /**
     * Check if a customer already has an active (PENDING or COUNTER_OFFER) offer
     * on this specific bike. Prevents duplicate concurrent negotiations.
     */
    @Query("SELECT COUNT(o) > 0 FROM BikeOffer o " +
           "WHERE o.customer.id = :customerId " +
           "AND o.bikeInventory.id = :bikeInventoryId " +
           "AND o.status IN (com.autoconsultancy.entity.BikeOffer$OfferStatus.PENDING, " +
           "                 com.autoconsultancy.entity.BikeOffer$OfferStatus.COUNTER_OFFER)")
    boolean existsActiveOffer(@Param("customerId") Long customerId,
                              @Param("bikeInventoryId") Long bikeInventoryId);

    /** Count by status — for dashboard stats */
    long countByStatus(OfferStatus status);

    /** Find active offers for a specific bike (for admin view) */
    @Query("SELECT o FROM BikeOffer o " +
           "WHERE o.bikeInventory.id = :bikeInventoryId " +
           "AND o.status IN (com.autoconsultancy.entity.BikeOffer$OfferStatus.PENDING, " +
           "                 com.autoconsultancy.entity.BikeOffer$OfferStatus.COUNTER_OFFER) " +
           "ORDER BY o.createdAt DESC")
    List<BikeOffer> findActiveOffersByBike(@Param("bikeInventoryId") Long bikeInventoryId);

    /** Customer's active offer on a specific bike (for display on bike detail page) */
    @Query("SELECT o FROM BikeOffer o " +
           "WHERE o.customer.id = :customerId " +
           "AND o.bikeInventory.id = :bikeInventoryId " +
           "ORDER BY o.createdAt DESC")
    List<BikeOffer> findByCustomerAndBike(@Param("customerId") Long customerId,
                                          @Param("bikeInventoryId") Long bikeInventoryId);
}
