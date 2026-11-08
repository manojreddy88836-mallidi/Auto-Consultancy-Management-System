package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.CreateWorkerRequest;
import com.autoconsultancy.dto.response.DashboardStatsResponse;
import com.autoconsultancy.entity.Application.ApplicationStatus;
import com.autoconsultancy.entity.AuditLog;
import com.autoconsultancy.entity.Customer;
import com.autoconsultancy.entity.Role;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.entity.Worker;
import com.autoconsultancy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final CustomerRepository customerRepository;
    private final WorkerRepository workerRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;
    @Qualifier("emailTaskExecutor")
    private final Executor taskExecutor;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        // Run all count queries in parallel using the shared async executor.
        // Each count hits an indexed field → sub-millisecond per query.
        // Sequential execution was: ~10 round-trips * ~2ms = ~20ms.
        // Parallel execution: max(~2ms) ≈ ~3ms total.

        java.time.LocalDateTime startOfMonth = java.time.LocalDate.now()
                .withDayOfMonth(1).atStartOfDay();
        org.springframework.data.mongodb.core.query.Query monthQuery =
                new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria
                                .where("createdAt").gte(Objects.requireNonNull(startOfMonth)));

        CompletableFuture<Long> totalCustomers      = CompletableFuture.supplyAsync(customerRepository::count, taskExecutor);
        CompletableFuture<Long> totalWorkers        = CompletableFuture.supplyAsync(workerRepository::count, taskExecutor);
        CompletableFuture<Long> totalApplications   = CompletableFuture.supplyAsync(applicationRepository::count, taskExecutor);
        CompletableFuture<Long> pendingApps         = CompletableFuture.supplyAsync(applicationRepository::countPending, taskExecutor);
        CompletableFuture<Long> underReview         = CompletableFuture.supplyAsync(() -> applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW), taskExecutor);
        CompletableFuture<Long> approved            = CompletableFuture.supplyAsync(() -> applicationRepository.countByStatus(ApplicationStatus.APPROVED), taskExecutor);
        CompletableFuture<Long> rejected            = CompletableFuture.supplyAsync(() -> applicationRepository.countByStatus(ApplicationStatus.REJECTED), taskExecutor);
        CompletableFuture<Long> completed           = CompletableFuture.supplyAsync(() -> applicationRepository.countByStatus(ApplicationStatus.COMPLETED), taskExecutor);
        CompletableFuture<Long> financeVerif        = CompletableFuture.supplyAsync(() -> applicationRepository.countByStatus(ApplicationStatus.FINANCE_VERIFICATION), taskExecutor);
        CompletableFuture<Long> docsUnderReview     = CompletableFuture.supplyAsync(() -> documentRepository.countByStatus("UNDER_REVIEW"), taskExecutor);
        CompletableFuture<Long> newAppsThisMonth    = CompletableFuture.supplyAsync(() -> {
            try { return mongoTemplate.count(monthQuery, com.autoconsultancy.entity.Application.class); }
            catch (Exception e) { return 0L; }
        }, taskExecutor);
        CompletableFuture<Long> newCustomersThisMonth = CompletableFuture.supplyAsync(customerRepository::countNewThisMonth, taskExecutor);

        // Wait for all to complete
        CompletableFuture.allOf(
            totalCustomers, totalWorkers, totalApplications, pendingApps,
            underReview, approved, rejected, completed, financeVerif,
            docsUnderReview, newAppsThisMonth, newCustomersThisMonth
        ).join();

        return DashboardStatsResponse.builder()
                .totalCustomers(totalCustomers.join())
                .totalWorkers(totalWorkers.join())
                .totalApplications(totalApplications.join())
                .pendingApplications(pendingApps.join())
                .underReviewApplications(underReview.join())
                .approvedApplications(approved.join())
                .rejectedApplications(rejected.join())
                .completedApplications(completed.join())
                .financeVerificationPending(financeVerif.join())
                .documentsUnderReview(docsUnderReview.join())
                .newApplicationsThisMonth(newAppsThisMonth.join())
                .newCustomersThisMonth(newCustomersThisMonth.join())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<Customer> getCustomers(@NonNull Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(@NonNull Long id) {
        return customerRepository.findById(id).orElseThrow();
    }

    @Transactional
    public void updateCustomerStatus(@NonNull Long id, boolean active) {
        Customer c = getCustomer(id);
        c.getUser().setActive(active);
        customerRepository.save(c);
    }

    @Transactional(readOnly = true)
    public Page<Worker> getWorkers(@NonNull Pageable pageable) {
        return workerRepository.findAll(pageable);
    }

    @Transactional
    public Worker createWorker(CreateWorkerRequest request) {
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.WORKER)
                .active(true)
                .build();
        user = userRepository.save(Objects.requireNonNull(user));

        Worker worker = Worker.builder()
                .id(Objects.requireNonNull(user.getId(), "Saved user must have an id"))
                .user(user)
                .employeeId(request.getEmployeeId())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .joiningDate(request.getJoiningDate())
                .active(true)
                .build();
        return workerRepository.save(Objects.requireNonNull(worker));
    }

    @Transactional(readOnly = true)
    public Worker getWorker(@NonNull Long id) {
        return workerRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Worker updateWorker(@NonNull Long id, CreateWorkerRequest request) {
        Worker worker = getWorker(id);
        User user = Objects.requireNonNull(worker.getUser());

        // Only update fields that are provided (preserve email/password if not sent)
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getEmail()     != null && !request.getEmail().isBlank()) user.setEmail(request.getEmail());
        if (request.getPhone()     != null) user.setPhone(request.getPhone());
        if (request.getPassword()  != null && !request.getPassword().isEmpty())
            user.setPassword(passwordEncoder.encode(request.getPassword()));

        if (request.getEmployeeId()  != null) worker.setEmployeeId(request.getEmployeeId());
        if (request.getDepartment()  != null) worker.setDepartment(request.getDepartment());
        if (request.getDesignation() != null) worker.setDesignation(request.getDesignation());
        if (request.getJoiningDate() != null) worker.setJoiningDate(request.getJoiningDate());

        userRepository.save(Objects.requireNonNull(user));
        return workerRepository.save(worker);
    }

    @Transactional
    public void deactivateWorker(@NonNull Long id) {
        Worker worker = getWorker(id);
        worker.setActive(false);
        worker.getUser().setActive(false);
        workerRepository.save(worker);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(@NonNull Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
