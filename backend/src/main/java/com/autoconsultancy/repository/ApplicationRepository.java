package com.autoconsultancy.repository;

import com.autoconsultancy.entity.Application;
import com.autoconsultancy.entity.Application.ApplicationStatus;
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

    @Query("{'workerAssignment.worker.user.id': ?0, 'workerAssignment.active': true}")
    List<Application> findByWorkerUserId(Long userId);

    long countByStatus(ApplicationStatus status);

    long countByCustomerIdAndStatus(Long customerId, ApplicationStatus status);

    long countByCustomerId(Long customerId);

    @Query(value = "{'workerAssignment.worker.user.id': ?0, 'workerAssignment.active': true}", count = true)
    long countActiveAssignmentsByWorkerUserId(Long userId);

    @Query(value = "{'workerAssignment.worker.user.id': ?0, 'workerAssignment.active': true, 'status': {$in: ['DOCUMENT_VERIFICATION', 'FINANCE_VERIFICATION']}}", count = true)
    long countPendingVerificationByWorkerUserId(Long userId);

    @Query(value = "{'workerAssignment.worker.user.id': ?0, 'workerAssignment.active': true, 'status': 'COMPLETED'}", count = true)
    long countCompletedByWorkerUserId(Long userId);

    @Query(value = "{'status': {$nin: ['APPROVED', 'REJECTED', 'COMPLETED', 'DRAFT']}}", count = true)
    long countPending();

    List<Application> findByStatusNotIn(Collection<ApplicationStatus> statuses);

    default Long findMaxId() {
        List<Application> list = findAll();
        return list.stream().mapToLong(Application::getId).max().orElse(0L);
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
