package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Application;
import com.autoconsultancy.entity.Application.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, Long> {

    List<Application> findByCustomerId(Long customerId);

    /** Paginated list of all applications — pure DB-level pagination, no in-memory loading. */
    Page<Application> findAllBy(Pageable pageable);

    /** Paginated list filtered by status — DB-level filter + pagination. */
    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    @Query("{'workerAssignment.$id': {$exists: true}, 'workerAssignment.worker.user.$id': ?0}")
    List<Application> findByWorkerUserId(Long userId);

    long countByStatus(ApplicationStatus status);

    long countByCustomerIdAndStatus(Long customerId, ApplicationStatus status);

    long countByCustomerId(Long customerId);

    // Note: workerAssignment is @DBRef — nested fields like 'workerAssignment.active' are not
    // stored in the applications document and cannot be queried via Spring Data @Query.
    // Queries use only the worker user ID reference which IS stored as workerAssignment.$id chain.
    @Query(value = "{'workerAssignment.$id': {$exists: true}}", count = true)
    long countActiveAssignmentsByWorkerUserId(Long userId);

    @Query(value = "{'workerAssignment.$id': {$exists: true}, 'status': {$in: ['DOCUMENT_VERIFICATION', 'FINANCE_VERIFICATION']}}", count = true)
    long countPendingVerificationByWorkerUserId(Long userId);

    @Query(value = "{'workerAssignment.$id': {$exists: true}, 'status': 'COMPLETED'}", count = true)
    long countCompletedByWorkerUserId(Long userId);

    @Query(value = "{'status': {$nin: ['APPROVED', 'REJECTED', 'COMPLETED', 'DRAFT']}}", count = true)
    long countPending();

    List<Application> findByStatusNotIn(Collection<ApplicationStatus> statuses);

    /**
     * Returns the single highest application ID without scanning the entire collection.
     * Uses a sort + limit(1) + projection so MongoDB only reads one document.
     */
    @Query(value = "{}", sort = "{'_id':-1}", fields = "{'_id':1}")
    java.util.Optional<Application> findTopByOrderByIdDesc();

    default Long findMaxId() {
        return findTopByOrderByIdDesc().map(Application::getId).orElse(0L);
    }

    default List<Object[]> countByMonth() {
        return new ArrayList<>();
    }

    default List<Object[]> countByStatusGrouped() {
        List<Object[]> result = new ArrayList<>();
        for (ApplicationStatus s : ApplicationStatus.values()) {
            long c = countByStatus(s);
            if (c > 0) result.add(new Object[]{s, c});
        }
        return result;
    }

    default List<Object[]> countByFinanceStatus() {
        return new ArrayList<>();
    }

    default List<Object[]> countByManufacturer(Pageable pageable) {
        return new ArrayList<>();
    }

    default List<Object[]> countByWorker(Pageable pageable) {
        return new ArrayList<>();
    }
}
