package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Application;
import com.autoconsultancy.entity.Application.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    List<Application> findByCustomerId(Long customerId);

    @Query("SELECT a FROM Application a JOIN a.workerAssignment wa " +
           "WHERE wa.worker.user.id = :userId AND wa.active = true")
    List<Application> findByWorkerUserId(@Param("userId") Long userId);

    long countByStatus(ApplicationStatus status);

    long countByCustomerIdAndStatus(Long customerId, ApplicationStatus status);

    long countByCustomerId(Long customerId);

    // Count active assignments for a given worker user ID
    @Query("SELECT COUNT(a) FROM Application a JOIN a.workerAssignment wa " +
           "WHERE wa.worker.user.id = :userId AND wa.active = true")
    long countActiveAssignmentsByWorkerUserId(@Param("userId") Long userId);

    // Count pending doc/finance review for worker
    @Query("SELECT COUNT(a) FROM Application a JOIN a.workerAssignment wa " +
           "WHERE wa.worker.user.id = :userId AND wa.active = true " +
           "AND a.status IN (com.autoconsultancy.entity.Application$ApplicationStatus.DOCUMENT_VERIFICATION, " +
           "com.autoconsultancy.entity.Application$ApplicationStatus.FINANCE_VERIFICATION)")
    long countPendingVerificationByWorkerUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM Application a JOIN a.workerAssignment wa " +
           "WHERE wa.worker.user.id = :userId AND wa.active = true " +
           "AND a.status = com.autoconsultancy.entity.Application$ApplicationStatus.COMPLETED")
    long countCompletedByWorkerUserId(@Param("userId") Long userId);

    // Monthly applications: returns [monthNum, count]
    @Query("SELECT MONTH(a.createdAt), COUNT(a) FROM Application a " +
           "WHERE YEAR(a.createdAt) = YEAR(CURRENT_DATE) " +
           "GROUP BY MONTH(a.createdAt) ORDER BY MONTH(a.createdAt)")
    List<Object[]> countByMonth();

    // Status distribution
    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> countByStatusGrouped();

    // Finance vs no finance
    @Query("SELECT fd.underFinance, COUNT(fd) FROM FinanceDetail fd GROUP BY fd.underFinance")
    List<Object[]> countByFinanceStatus();

    // Applications by manufacturer (top 7) - LEFT JOIN to handle apps without bikeDetail
    @Query("SELECT m.name, COUNT(a) FROM Application a " +
           "LEFT JOIN a.bikeDetail bd LEFT JOIN bd.manufacturer m " +
           "WHERE m IS NOT NULL " +
           "GROUP BY m.name ORDER BY COUNT(a) DESC")
    List<Object[]> countByManufacturer(Pageable pageable);

    // Applications per worker
    @Query("SELECT u.firstName, u.lastName, COUNT(a) FROM Application a " +
           "JOIN a.workerAssignment wa JOIN wa.worker w JOIN w.user u " +
           "GROUP BY u.firstName, u.lastName ORDER BY COUNT(a) DESC")
    List<Object[]> countByWorker(Pageable pageable);

    // Pending (not finished) count
    @Query("SELECT COUNT(a) FROM Application a WHERE a.status NOT IN " +
           "(com.autoconsultancy.entity.Application$ApplicationStatus.APPROVED, " +
           "com.autoconsultancy.entity.Application$ApplicationStatus.REJECTED, " +
           "com.autoconsultancy.entity.Application$ApplicationStatus.COMPLETED, " +
           "com.autoconsultancy.entity.Application$ApplicationStatus.DRAFT)")
    long countPending();

    /** Returns the highest existing primary-key value, or null if the table is empty.
     *  Used by generateApplicationNumber() to avoid duplicate key collisions after deletions. */
    @Query("SELECT MAX(a.id) FROM Application a")
    Long findMaxId();
}

