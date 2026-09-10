package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.CustomerProfileRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.CustomerDashboardStats;
import com.autoconsultancy.entity.Customer;
import com.autoconsultancy.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CUSTOMER')")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Customer>> getProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getProfile(authentication.getName()), "Profile fetched"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Customer>> updateProfile(@RequestBody CustomerProfileRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.updateProfile(authentication.getName(), request), "Profile updated"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<CustomerDashboardStats>> getDashboardStats(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getDashboardStats(authentication.getName()), "Dashboard fetched"));
    }
}
