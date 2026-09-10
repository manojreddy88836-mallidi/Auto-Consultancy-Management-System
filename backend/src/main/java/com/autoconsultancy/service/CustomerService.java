package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.CustomerProfileRequest;
import com.autoconsultancy.dto.response.CustomerDashboardStats;
import com.autoconsultancy.entity.Application.ApplicationStatus;
import com.autoconsultancy.entity.Customer;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.repository.ApplicationRepository;
import com.autoconsultancy.repository.CustomerRepository;
import com.autoconsultancy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public Customer getProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return customerRepository.findById(user.getId()).orElseThrow();
    }

    @Transactional
    public Customer updateProfile(String email, CustomerProfileRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Customer customer = customerRepository.findById(user.getId()).orElseThrow();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        userRepository.save(user);

        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setPincode(request.getPincode());
        customer.setIdentityProof(request.getIdentityProof());
        customer.setIdentityProofNumber(request.getIdentityProofNumber());
        customer.setProfileComplete(true);

        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDashboardStats getDashboardStats(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Long customerId = user.getId();

        long total     = applicationRepository.countByCustomerId(customerId);
        long draft     = applicationRepository.countByCustomerIdAndStatus(customerId, ApplicationStatus.DRAFT);
        long submitted = applicationRepository.countByCustomerIdAndStatus(customerId, ApplicationStatus.SUBMITTED);
        long approved  = applicationRepository.countByCustomerIdAndStatus(customerId, ApplicationStatus.APPROVED);
        long rejected  = applicationRepository.countByCustomerIdAndStatus(customerId, ApplicationStatus.REJECTED);
        long completed = applicationRepository.countByCustomerIdAndStatus(customerId, ApplicationStatus.COMPLETED);
        // Pending = anything in-progress (not draft, submitted, approved, rejected, completed)
        long pending   = Math.max(total - draft - submitted - approved - rejected - completed, 0);

        return CustomerDashboardStats.builder()
                .totalApplications(total)
                .draftApplications(draft)
                .submittedApplications(submitted)
                .pendingApplications(pending)
                .approvedApplications(approved)
                .rejectedApplications(rejected)
                .completedApplications(completed)
                .build();
    }
}
