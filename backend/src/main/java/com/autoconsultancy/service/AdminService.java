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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        return DashboardStatsResponse.builder()
                .totalCustomers(customerRepository.count())
                .totalWorkers(workerRepository.count())
                .totalApplications(applicationRepository.count())
                .pendingApplications(applicationRepository.countPending())
                .underReviewApplications(applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW))
                .approvedApplications(applicationRepository.countByStatus(ApplicationStatus.APPROVED))
                .rejectedApplications(applicationRepository.countByStatus(ApplicationStatus.REJECTED))
                .completedApplications(applicationRepository.countByStatus(ApplicationStatus.COMPLETED))
                .financeVerificationPending(applicationRepository.countByStatus(ApplicationStatus.FINANCE_VERIFICATION))
                .documentsUnderReview(documentRepository.countByStatus("UNDER_REVIEW"))
                .newApplicationsThisMonth(applicationRepository.countByMonth().stream()
                        .filter(r -> {
                            int m = ((Number) r[0]).intValue();
                            return m == java.time.LocalDate.now().getMonthValue();
                        })
                        .mapToLong(r -> ((Number) r[1]).longValue()).sum())
                .newCustomersThisMonth(customerRepository.countNewThisMonth())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<Customer> getCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(Long id) {
        return customerRepository.findById(id).orElseThrow();
    }

    @Transactional
    public void updateCustomerStatus(Long id, boolean active) {
        Customer c = getCustomer(id);
        c.getUser().setActive(active);
        customerRepository.save(c);
    }

    @Transactional(readOnly = true)
    public Page<Worker> getWorkers(Pageable pageable) {
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
        user = userRepository.save(user);

        Worker worker = Worker.builder()
                .user(user)
                .employeeId(request.getEmployeeId())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .joiningDate(request.getJoiningDate())
                .active(true)
                .build();
        return workerRepository.save(worker);
    }

    @Transactional(readOnly = true)
    public Worker getWorker(Long id) {
        return workerRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Worker updateWorker(Long id, CreateWorkerRequest request) {
        Worker worker = getWorker(id);
        User user = worker.getUser();

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

        userRepository.save(user);
        return workerRepository.save(worker);
    }

    @Transactional
    public void deactivateWorker(Long id) {
        Worker worker = getWorker(id);
        worker.setActive(false);
        worker.getUser().setActive(false);
        workerRepository.save(worker);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
